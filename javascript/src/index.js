import 'es6-shim';
import 'whatwg-fetch';
import Symbol from 'es-symbol';
import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import 'react-select/dist/react-select.css';
import njs from 'nipplejs';

if (!window.Symbol) {
  window.Symbol = Symbol;
}
window.$ = $;
window.jQuery = $;
require('bootstrap/dist/js/bootstrap.min');

class App extends React.Component {

    constructor(props) {
      super(props);
    }

    componentDidMount(){
        this.draw();
    }

    draw = () => {
        const ctx = this.canvasRef.getContext("2d");
        this.moveBullet();
        ctx.clearRect(0, 0, this.canvasRef.width, this.canvasRef.height);
        for(var player in this.props.data.players) {
            var p = this.props.data.players[player];
            ctx.save();
            ctx.translate(p.posX,-p.posY);
            ctx.rotate(-p.angle*Math.PI/180);
            this.ships(ctx,p.color);
            ctx.restore();
        }
        for(var bullet in this.props.data.bullets) {
            var b = this.props.data.bullets[bullet];
            ctx.save();
            ctx.translate(b.posX, -b.posY);
            ctx.rotate(-b.angle*Math.PI/180);
            this.bullet(ctx);
            ctx.restore();
        }
        //this.drawShips(ctx, this.props.data.players);
        setTimeout(this.draw, 100);
    }

    ships = (ctx, fillStyle) => {
        ctx.fillStyle = fillStyle;
        ctx.globalAplha = 1.0;
        ctx.beginPath();
        ctx.moveTo(25,25);
        ctx.lineTo(-25,-25);
        ctx.lineTo(25,-25);
        ctx.lineTo(-25,25);
        ctx.closePath();
        ctx.fill();
    }

    bullet = (ctx) => {
        ctx.beginPath();
        ctx.fillStyle = "black";
        ctx.globalAlpha = 1.0;
        ctx.beginPath();
        ctx.moveTo(10,5);
        ctx.lineTo(-10,5);
        ctx.lineTo(-10,-5);
        ctx.lineTo(10,-5);
        ctx.closePath();
        ctx.fill();
    }

    moveBullet = () => {
        fetch('/mvB', {
            method: 'POST',
            header: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: '{}'
        });
    }

    render() {
        var dataJ = JSON.stringify(this.props.data);
        var rows = [];
        // Opti possible ???
        for(var score in this.props.data.leaderboard) {
            rows.push(score+": "+this.props.data.leaderboard[score]);
        }

        return (
            <div>
              <canvas height="600" width="800" ref={ref => this.canvasRef = ref} />
              {dataJ}
              <Leaderboard rows={rows}/>
            </div>
        );
    }
}

const stylesJoystick = {
    border: '2px solid black',
    backgroundColor: '#adadad',
    height: '120px'
};
const stylesTir = {
    border: '2px solid black',
    backgroundColor: '#afd9ee',
    height: '120px'
};

class Joystick extends React.Component {
    constructor(props) {
        super(props);
        this.joystickData = '';
    }

    move() {
        if(this.joystickData) {
            var x = 0;
            var y = 0;
            if(this.joystickData.distance > 10) {
                if (this.joystickData.angle.degree < 45 || this.joystickData.angle.degree > 315) {
                    x = 1;
                } else if (this.joystickData.angle.degree < 135) {
                    y = 1;
                } else if (this.joystickData.angle.degree < 225) {
                    x = -1;
                } else {
                    y = -1;
                }
                console.log(x + ' ............... ' + y);
                console.log(this.joystickData.angle.radian + ' / ' + this.joystickData.angle.degree + ' : ' + this.joystickData.distance);
                fetch('/mvP/' + this.props.name + '/' + Math.cos(this.joystickData.angle.radian) + '/' + Math.sin(this.joystickData.angle.radian) + '/' + this.joystickData.angle.degree, {
                    method: 'POST',
                    header: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    },
                    body: '{}'
                });
            }
        }
    }

    shoot() {
        var indexP;
        for(var player in this.props.data.players) {
            if(this.props.data.players[player].name == this.props.name) {
                indexP = player;
            }
        }
        var p = this.props.data.players[indexP];
        var x = p.posX;
        var y = p.posY;
        var a = p.angle;
        fetch('/addB/' + x + '/' + y + '/' + a, {
            method: 'POST',
            header: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: '{}'
        });
    }

    componentDidMount() {
        var that = this;

        // ZONE DE CONTROLE
        const joystickParams = {
            zone: document.getElementById("joystick"),
            color: "blue"
        };
        var manager = njs.create(joystickParams);
        manager.on('added', function(evt, nipple) {
            that.interval = setInterval(() => that.move(), 1000/60);
            nipple.on('move', function(evt, data) {
                that.joystickData = data;
            });
        }).on('removed', function(evt) {
            if(that.interval) {
                window.clearInterval(that.interval);
            }
        });

        // ZONE DE TIR
        const tirParams = {
            zone: document.getElementById("tir"),
            color: "blue"
        };
        const manager2 = njs.create(tirParams);
        manager2.on('added', function(evt, nipple) {
            that.shoot();
        });
    }

    render() {

        return (
        <div>
            <div className="col-xs-6" style={stylesJoystick} id="joystick"></div>
            <div className="col-xs-6" style={stylesTir} id="tir"></div>
        </div>
        );
    }
}

class Leaderboard extends React.Component {
    constructor(props) {
        super(props);
    }
    render() {
        const rows = this.props.rows.map(row => {
            return (
                <li key={row.toString()}>{row}</li>
            );
        });
        return (
            <div>{rows}</div>
        );
    }
}

export function init(node, data) {
  ReactDOM.render(<App data={data} />, node);
}

export function joystick(node, data, name) {
    ReactDOM.render(<Joystick data={data} name={name} />, node);
}