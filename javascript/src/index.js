import 'es6-shim';
import 'whatwg-fetch';
import Symbol from 'es-symbol';
import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import 'react-select/dist/react-select.css';
import njs from 'nipplejs';
import 'react-router';

if (!window.Symbol) {
  window.Symbol = Symbol;
}
window.$ = $;
window.jQuery = $;
require('bootstrap/dist/js/bootstrap.min');

class Board extends React.Component {

    constructor(props) {
        super(props);
    }

    componentDidMount() {
        this.draw();
    }

    draw = () => {
        if(this.props.data) {
            const ctx = this.canvasRef.getContext("2d");
            ctx.clearRect(0, 0, this.canvasRef.width, this.canvasRef.height);
            for(var ship in this.props.data.ships) {
                var s = this.props.data.ships[ship];
                ctx.save();
                ctx.translate(s.posX,-s.posY);
                ctx.rotate(-s.angle*Math.PI/180);
                this.ships(ctx,s.color,s.life);
                ctx.restore();

                /*var score = this.props.data.ships[ship].score;
                if(this.props.data.bestScore < score) {
                    fetch('/modifS/' + this.props.data.ships[ship].name + '/' + score, {
                        method: 'POST',
                        header: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: '{}'
                    });
                }*/

            }
            for(var bullet in this.props.data.bullets) {
                var b = this.props.data.bullets[bullet];
                ctx.save();
                ctx.translate(b.posX, -b.posY);
                ctx.rotate(-b.angle*Math.PI/180);
                this.bullet(ctx);
                ctx.restore();
            }
            ctx.font = "20px Arial";
            var text = this.props.data.nameScore + " - " + this.props.data.bestScore
            ctx.fillText(text,800-40*this.props.data.nameScore.length,30);
        }
        setTimeout(this.draw, 100);
    }

    ships = (ctx,fillStyle,life) => {
        ctx.fillStyle = fillStyle;
        ctx.globalAplha = 1.0;
        ctx.beginPath();
        ctx.moveTo(20,0);
        ctx.lineTo(-20,-18);
        ctx.lineTo(-20,18);
        ctx.closePath();
        ctx.fill();
        var opacityShield = 0;
        switch(life) {
            case 3 : opacityShield = 0.5;break;
            case 2 : opacityShield = 0.3;break;
            case 1 : opacityShield = 0;break;
        }
        ctx.fillStyle="rgba(23,145,167,"+opacityShield+")";
        ctx.arc(-4,0,40,0,Math.PI*2,true);
        ctx.fill();
    }

    bullet = (ctx) => {
        ctx.fillStyle = "black";
        ctx.globalAlpha = 1.0;
        ctx.beginPath();
        ctx.moveTo(5,2);
        ctx.lineTo(-5,2);
        ctx.lineTo(-5,-2);
        ctx.lineTo(5,-2);
        ctx.closePath();
        ctx.fill();
    }

    render() {
        var rows = [];
        if(this.props.data) {
            var dataJ = JSON.stringify(this.props.data);

            // Opti possible ???
            for(var player in this.props.data.players) {
                rows.push(this.props.data.players[player].name+": "+this.props.data.players[player].score);
            }
        }
        return (
            <div>
              <canvas height="600" width="800" ref={ref => this.canvasRef = ref} />
              <Leaderboard rows={rows}/>
            </div>
        );
    }
}

const stylesJoystick = {
    border: '1px solid black',
    backgroundImage: "url("+"/assets/images/joystick.png"+")",
    backgroundSize: "contain",
    backgroundPosition: "center",
    backgroundRepeat: "no-repeat",
    height: '120px'
};
const stylesTir = {
    border: '1px solid black',
    backgroundImage: "url("+"/assets/images/cible.png"+")",
    backgroundSize: "contain",
    backgroundPosition: "center",
    backgroundRepeat: "no-repeat",
    height: '120px'
};

class Joystick extends React.Component {
    constructor(props) {
        super(props);
        this.joystickData = '';
        this.color = '000000';
        this.connection = new WebSocket("ws://localhost:9000/echo");
        this.connection.onopen = evt => {
            console.log("open");
        };
        this.connection.onclose = evt => {
            console.log("close");
        };
        this.connection.onmessage = evt => {
            console.log("receive");
        };
    }

    move() {
        if(this.joystickData) {
            if(this.joystickData.distance > 10) {
                let js = JSON.stringify({"action": "moveShip","id": this.props.ship.id.toString(), "angle": this.joystickData.angle.radian.toString()});
                this.connection.send(js);
            }
        }
    }

    shoot() {
        let js = JSON.stringify({"action": "addBullet", "id": this.props.ship.id.toString()});
        this.connection.send(js);
    }

    componentWillMount() {
        document.body.addEventListener('keyup', this.handleInput.bind(this), false);
    }

    handleInput(e) {
        if(event.keyCode == '32') {
            this.shoot();
        }
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
            that.interval = setInterval(() => that.move(), 100);
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
        var checkID=true;
        for(var ship in this.props.data.ships) {
            var s = this.props.data.ships[ship];
            if(s.id == this.props.ship.id) {
                checkID = true;
            }
        }
        if(checkID) {
            return (
                <div>
                    <div className="col-xs-6" style={stylesJoystick} id="joystick" ></div>
                    <div className="col-xs-6" style={stylesTir} id="tir"></div>
                </div>
            );
        } else {
            this.connection.onclose();
            document.location.href = "/res/"+ this.props.ship.name +'/'+ this.props.ship.color/*.slice(1)*/;
            return false;
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
    if(data == null) {
        ReactDOM.render(<Board />,node);
    }
  ReactDOM.render(<Board data={data} />, node);
}

export function joystick(node, data, ship) {
    ReactDOM.render(<Joystick data={data} ship={ship} />, node);
}