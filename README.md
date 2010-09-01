= Scapulet 
  
  Scapulet 
  
Scapulet is an XMPP component library that currently supports XEP-0114 (http://xmpp.org/extensions/xep-0114.html).  
It uses the standard scala library for parsing xml so you can match against it using the xml snippets.
It uses akka for concurrency and fault-tolerance.

The main motivation for writing this was that all libraries I used so far only work well if you want to write a bot, or
don't work with all jabber clients etc. Most of them also insist on writing their own xml parser where IMO that's not
the place where most of the time is spent.

An example of the intended usage can be found in this gist: http://gist.github.com/557204


== Note on Patches/Pull Requests
 
* Fork the project.
* Make your feature addition or bug fix.
* Add tests for it. This is important so I don't break it in a
  future version unintentionally.
* Commit, do not mess with sbt project, version, or history.
  (if you want to have your own version, that is fine but bump version in a commit by itself I can ignore when I pull)
* Send me a pull request. Bonus points for topic branches.

== Copyright

Copyright (c) 2010 Mojolly. See LICENSE for details.
