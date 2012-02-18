# Scapulet
  
Scapulet is an XMPP library that currently supports XEP-0114 for components and implements an XMPP Client connection for writing bots.
It uses the standard scala library for parsing xml so you can match against it using the xml snippets.
It uses akka for concurrency and fault-tolerance.

There is a google group for your questions: [Mailing List](http://groups.google.com/group/scapulet-user)
There is also an irc channel: <irc://irc.freenode.net/scapulet>  

#### Implemented:

* [XMPP-Core](http://xmpp.org/rfcs/rfc3920.html) (partial)
* [IM & Presence](<http://xmpp.org/rfcs/rfc3921.html) (partial)
* [XEP 0114: component connections](http://xmpp.org/extensions/xep-0114.html)
* [XEP 0030: Service discovery](http://xmpp.org/extensions/xep-0030.html) (partial)


## Building an XMPP Component

To create a component you first create a component connection, to do this we need to have a config defined.

```
scapulet {
  components {
    weather {  # this config will create a component at weather.jabber.local
      name = WeatherComponent
      description = "A component that echos whatever you send it"
      host = "127.0.0.1"
      port = 34827
      userName = "weather"
      password = "demo"
      domain = "jabber.local"
    }
  }
}
```

With that config you can get a hold of the component handle which you can use to register the handlers that will make up
your actual component. Those handlers will be made so they too are configurable.

```scala
import io.backchat.scapulet._
import akka.actor._
implicit val system = ActorSystem("xmpp_components")
val weather = system.scapulet.component("weather")
```

You can then write to the component by sending it xml snippets

```scala
system.actorFor("/user/xmpp/components/weather") ! <presence to="julliet@capulet.com" from="echo.montague.net"></presence>
```

So far you've only got a connection, you still need to make the component respond to actual xmpp stanzas. For this we
have to register handlers. To register a handler we need a predicate and an actor to actually handle the stanzas.

```scala
echoComponent.register(new ScapuletHandler("allStanzas") {

  val features = Seq.empty[Feature]
  val identities = Seq.empty[Identity]

  def handleStanza = {
    case stanza => replyWith(stanza)
  }
})
```

The example won't actually work in an XMPP context, we would have to switch the from and to addresses.

## Note on Patches/Pull Requests
 
* Fork the project.
* Make your feature addition or bug fix.
* Add tests for it. This is important so I don't break it in a
  future version unintentionally.
* Commit, do not mess with sbt project, version, or history.
  (if you want to have your own version, that is fine but bump version in a commit by itself I can ignore when I pull)
* Send me a pull request. Bonus points for topic branches.

## Copyright

Copyright (c) 2010 Mojolly Ltd. See the [LICENSE](https://github.com/mojolly/scapulet/raw/HEAD/LICENSE) for details.
