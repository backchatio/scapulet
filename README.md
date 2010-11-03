= Scapulet 
  
  Scapulet 
  
Scapulet is an XMPP component library that currently supports XEP-0114 (http://xmpp.org/extensions/xep-0114.html).  
It uses the standard scala library for parsing xml so you can match against it using the xml snippets.
It uses akka for concurrency and fault-tolerance.

The main motivation for writing this was that all libraries I used so far only work well if you want to write a bot, or
don't work with all jabber clients etc. Most of them also insist on writing their own xml parser where IMO that's not
the place where most of the time is spent.

I've used this library to create a MUC component which we needed and now I need to create a pubsub component so I'm finding 
which things need to be shared and those are being put in this library.   
Currently I'm working towards implementing XEP-0060

There is a google group for your questions: <http://groups.google.com/group/scapulet-user>  
There is also an irc channel: <irc://irc.freenode.net/scapulet>  

implemented:
XMPP-Core: <http://xmpp.org/rfcs/rfc3920.html> (partial)  
IM & Presence: <http://xmpp.org/rfcs/rfc3921.html> (partial)  
XEP 0114: component connections <http://xmpp.org/extensions/xep-0114.html>  
XEP 0030: Service discovery (partial) <http://xmpp.org/extensions/xep-0030.html>  

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
