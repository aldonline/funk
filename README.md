funk
====

Resolve lazy properties by trying different alternatives.

Handles cyclic dependencies and performs automatic backtracking when a better solution path is found

TODO: docs, tests, etc. ( this project was recently refactored out of a larger codebase )

In the meantime, here's an example from a production project.


```scala


	class Person( e:DEntity ) extends BaseEntity( e ) with FunkContext {

	  object common {
	    val image   				= funky( local.image.get ).or( angellist.profile.get.image.get )
        val name    				= funky( local.name.get ).or( angellist.name.get ).or( facebook.name.get ) 
        val bio     				= funky( angellist.bio )
        val bioHtml 				= funky[NodeSeq]( <div>{ bio.get }</div> ).or( NodeSeq.Empty )
        val twitter_username 		= funky( angellist.twitter_username )
        val twitter_username_html 	= funky[NodeSeq]( <p>{twitter_username.get}</p> ).or( NodeSeq.Empty )
	  } 
	  
	  object local {
	    val name  = funky( e.get( Schema.common_name ).get )
        val image = funky( e.get( Schema.common_image ).get )
	  }
	  
	  object angellist {
	    val profile 		= funky( AngellistService.getUserSync( slug.eitherValue( id ) ).get  )
	    val id 				= funky( e.get( Schema.ext_angellistid ).map(_.toInt).get )
	    val slug 			= funky( e.get( Schema.ext_angellistslug ).get )
	    val name			= funky( angellist.profile.get.name )
	    val bio				= funky( profile.get.bio.get )
	    
	    val twitter_username = funky( profile.get.twitter_url.get.split("/").last )
	  }

	  object facebook {
	    val id 			= funky( e.get( Schema.ext_facebookid ).map(_.toInt).get )
	    val name		= funky("facebookname")
	  }
	  
	  override lazy val image = common.image.asOption
	  override lazy val name = common.name.get
	  
	  lazy val html = <div>
          <h1>{name}</h1>
		  { profileImageHTML }
		  { common.bioHtml.get }
		  { common.twitter_username_html.get }
	    </div>
	}


```
