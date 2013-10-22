package com.southup.funk

import scala.collection.mutable.{ArrayBuffer, HashSet, HashMap, ArrayStack}

sealed trait Solution[+T] {
  def isDefined = false
}
case class Ok[T]( v: T ) extends Solution[T] {
  override def isDefined = true
}
case object KO extends Solution[Nothing]
case object Pending extends Solution[Nothing]

sealed trait SolutionError extends Throwable
case object NoSolutionError extends SolutionError
case object PendingSolutionError extends SolutionError

trait FunkContext { ctx =>

  private var depth = 0
  private def incDepth() { depth = depth + 1 }
  private def decDepth() { depth = depth - 1 }
  private def buildIndent = ( 0 to depth ).toList.map( i => "  " ) mkString ""

  // public config ( override this to turn on debugging )
  def funkDebug = false

  // keys that are currently being solved
  def avoidingCycles[T]( k: Key[T] )( f: => Solution[T] ): Solution[T] =
    if ( k.pending ) {
      Pending
    } else {
      k.pending = true
      val ret = f
      k.pending = false
      ret
    }

  private def withDebug[T]( k: Key[T] )( f: => Solution[T] ): Solution[T] =
    if ( funkDebug ) {
      // TODO: add numbers to rules invocations
      // ( they become hard to track when they grow )
      val indent = buildIndent
      val name = k.name getOrElse "?"
      println( indent + name + "..." )
      incDepth()
      val ret = f
      decDepth()
      println( indent + name + " = " + ret )
      ret
    } else {
      f
    }

  private def solve[T]( key: Key[T] ): Solution[T] = synchronized {
    withDebug( key ) { // prints debug messages
      avoidingCycles( key ) {
        // if we are already solving a key then we return unknown
        // ( prevents cyclic recursion )
        var hasNestedPendings = false
        for ( solver <- key.solvers.reverse ) {
          val result = try {
            Some( solver.exec() )
          } catch {
            case NoSolutionError => None
            case PendingSolutionError => {
              hasNestedPendings = true
              None
            }
            case e: Throwable => {
              // we are eating all exceptions here
              // this should be configurable
              if ( funkDebug ) {
                println( "--------> " + e )
              }
              None
            }
          }
          result match {
            case Some( x ) if ( x != null ) => return Ok( x )
            case _                          => // keep looping
          }
        }
        if ( hasNestedPendings ) Pending else KO
      }
    }
  }

  private class Solver[T]( f: () => T ) { def exec() = f() }

  class Key[T] private[FunkContext] (
      private[FunkContext] val solvers: List[Solver[T]] = Nil,
      private[FunkContext] val name: Option[String] = None ) {

    private[FunkContext] var cache: Option[Solution[T]] = None
    private[FunkContext] var pending: Boolean = false

    final def asOption = getSolution match {
      case Ok( v ) => Some( v )
      case _       => None
    }

    // solution is not functional. it may change
    final def getSolution = synchronized {
      cache match {
        case Some( x ) => x
        case None => ctx.solve( this ) match {
          case Pending => Pending
          case x => { cache = Some( x ) ; x }
        }
      }
    }

    final def get: T = getSolution match {
      case Pending => throw PendingSolutionError
      case KO      => throw NoSolutionError
      case Ok( v ) => v
    }

    private def addSolver( s: Solver[T] ): Key[T] = new Key(
      solvers = s :: solvers,
      name = name
    )

    // --- builders
    def name( str: String ): Key[T] = new Key(
      solvers = solvers,
      name = Some( str )
    )
    def or( f: => T ): Key[T] = addSolver( new Solver( () => f ) )

    // TODO: def or( other:Key[T] )
    def eitherValue[Q]( other: Key[Q] ): Either[T, Q] = getSolution match {
      case Ok( k ) => Left( k )
      case _       => Right( other.get )
    }

    override def toString =
      "Key(" + name.getOrElse( "" ) + ") = " +
        cache.map( _.toString ).getOrElse( "?" )

  }

  def funky[T]( f: => T ): Key[T] = new Key( List( new Solver( () => f ) ) )

  def funky[T] = new Key[T]( Nil )

}

object Sandbox extends FunkContext {

  override def funkDebug = true

  val a = funky( b.get ).name( "a" )
  val b: Key[String] = funky( a.get ).or( c.get ).name( "b" )
  val c = funky( a.get ).or( d.get ).name( "c" )
  val d = funky( "d" ).name( "d" )

  def test = {
    a
  }
}

