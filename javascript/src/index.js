import 'es6-shim';
import 'whatwg-fetch';
import Symbol from 'es-symbol';
import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import 'react-select/dist/react-select.css';

if (!window.Symbol) {
  window.Symbol = Symbol;
}
window.$ = $;
window.jQuery = $;
require('bootstrap/dist/js/bootstrap.min');

class Timer extends React.Component {
    constructor(props) {
        super(props);
        this.state = { secondsElapsed: 0 };
    }

    tick() {
        this.setState(prevState => ({
            secondsElapsed: prevState.secondsElapsed + 1
        }));
    }

    componentDidMount() {
        this.interval = setInterval(() => this.tick(), 1000);
    }

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    render() {
        return (
            <div>Seconds Elapsed: {this.state.secondsElapsed}</div>
        );
    }
}

class App extends React.Component {

    constructor(props) {
      super(props);
    }

    componentDidMount(){
        this.draw();
    }

    draw = () => {
        const ctx = this.canvasRef.getContext("2d");
        ctx.clearRect(0, 0, this.canvasRef.width, this.canvasRef.height);
        ctx.fillStyle = "black";
        ctx.font = "14px Arial";
        ctx.fillText(this.props.data,0,20);
        setTimeout(this.draw, 100);

    }

    render() {
      var dataJ = JSON.stringify(this.props.data);
      var rows = [];
      for(var score in this.props.data.leaderboard) {
          rows.push(score);
      }
      return (
          <div>
              <canvas height="600" width="800" ref={ref => this.canvasRef = ref} />
              {dataJ}
              aa{rows.keys()}aa
          </div>
      );
    }
}

export function init(node, data) {
  ReactDOM.render(<App data={data}/>, node);
  ReactDOM.render(<Timer />, document.getElementById("timer"));
}
