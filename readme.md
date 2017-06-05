# Spaceships

run using sbt

```sbt
$ ~run
```

then open the game board at `http://localhost:9000/master` on your laptop
and open the game pad at `http://localhost:9000/m` on your device

## TODO

* Use Play 2.5
* Use Akka Stream instead of Iteratees
* Rewrite the game model as an immutable state with reducer (like Redux)
* Mutate the game in a Flow of event (GamePadEvent, TickEvent, FireEvent, etc ...)
* Rewrite the JS part using a modern stack like ES6 + Babel + Webpack
* Rewrite the JS because it's kind of aweful
* Use React for the UI parts, keep canvas for the game inside a React component
* Make the canvas adapt to screen size (make it zoom, while having an internal known size)

## Explore

* Push the game on CleverCloud or Heroku and check if it works well
  * if not, tune the code to
    * avoid sending useless event
    * throttle events
    * etc ...
* Handle multiple game at the same time using
  * redis to persist game state
  * redis pub/sub for eventing
  * QR codes to join game
  * etc ...
