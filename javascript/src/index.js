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
        this.collisionBullet();
        this.collisionBulletShip();
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
        ctx.moveTo(5,5);
        ctx.lineTo(-5,5);
        ctx.lineTo(-5,-5);
        ctx.lineTo(5,-5);
        ctx.closePath();
        ctx.fill();
    }

    collisionBulletShip() {
        for(var bullet in this.props.data.bullets) {
            var b = this.props.data.bullets[bullet];
            var bul = {x: b.posX-5, y: b.posY+5, width: 10, height: 10};
            for(var player in this.props.data.players) {
                var p = this.props.data.players[player];
                var pla = {x: p.posX-25, y: p.posY-25, width: 50, height:50}
                if(bul.x < pla.x + pla.width &&
                    bul.x + bul.width > pla.x &&
                    bul.y < pla.y + pla.height &&
                    bul.height + bul.y > pla.y &&
                    b.nameShip != p.name) {
                    fetch('/dropP/'+p.name, {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: '{}'
                    });
                    fetch('/dropB/'+b.id, {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: '{}'
                    });
                    fetch('/addPnt/'+b.nameShip+'/1', {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: '{}'
                    });
                }
            }
        }
    }

    collisionBullet() {
        for(var bullet in this.props.data.bullets) {
            var b = this.props.data.bullets[bullet];
            if(b.posX > 800 || b.posX < 0 || b.posY < -600 || b.posY > 0) {
                fetch('/dropB/'+b.id, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    },
                    body: '{}'
                });
            }
        }
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
        for(var player in this.props.data.players) {
            rows.push(this.props.data.players[player].name+": "+this.props.data.players[player].score);
        }

        return (
            <div>
              <canvas height="600" width="800" ref={ref => this.canvasRef = ref} />
              <Leaderboard rows={rows}/>
              {dataJ}
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
            if(this.joystickData.distance > 10) {
                console.log(this.joystickData.angle.radian);
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
        fetch('/addB/' + p.posX + '/' + p.posY + '/' + p.angle + '/' + this.props.name, {
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
        var pName=false;
        for(var player in this.props.data.players) {
            if(this.props.data.players[player].name == this.props.name) {
                pName = true;
            }
        }
        console.log(pName);
        if(pName) {
            return (
                <div>
                    <div className="col-xs-6" style={stylesJoystick} id="joystick"></div>
                    <div className="col-xs-6" style={stylesTir} id="tir"></div>
                </div>
            );
        } else {
            document.location.href = "/m";
        }
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