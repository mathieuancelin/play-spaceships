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
/*
class Vector {
    constructor(x,y) {
        this.x = x;
        this.y = y;
    }

    additionVV(vect) {
        return new Vector(this.x + vect.x, this.y + vect.y);
    }

    additionVS(scalar) {
        return new Vector(this.x + scalar, this.y + scalar);
    }

    multiplicationVS(scalar) {
        return new Vector(this.x * scalar, this.y * scalar);
    }

    magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    normalized() {
        return new Vector(this.x/this.magnitude(), this.y/this.magnitude());
    }
}

class Physics {
    constructor() {
        this.prevDate = Date.now();
        this.deltaTime = 0.0;
        this.shipList = [];
        this.bulletList = [];
    }

    simulate(state) {
        let now = Date.now();
        this.deltaTime = now - this.prevDate;
        this.prevDate = now;
        // loop --> objet.simuler(this.deltaTime);
    }

    draw(ctx) {
        this.shipList.forEach(function(ship) {
            ship.draw(ctx);
        });
        this.bulletList.forEach(function(bullet) {
            bullet.draw(ctx);
        });
    }

    addShip(pos,color) {
        this.shipList.push(new Ship(pos,color));
    }

    getShip() {
        return this.shipList;
    }

    deleteShip() {

    }

    addBullet(pos) {
        this.bulletList.push(new Bullet(pos));
    }

    getBullet() {
        return this.bulletList;
    }

    deleteBullet() {

    }
}

class ObjectPhysics {
    constructor(pos, vitesse) {
        this.position = pos;
        this.angle = 0;
        this.vitesse = vitesse;
        this.velocity = new Vector(0,0);
        this.angularVelocity = 0;
        this.drag = 0.1;
        this.angularDrag = 0.1;
    }

    simulate(deltaTime) {
        this.position += this.velocity.additionVS(deltaTime);
        this.velocity += this.getDragForce(this.velocity,this.drag).multiplicationVS(deltaTime);
        this.angle += this.angularVelocity * deltaTime;
        this.angularVelocity += this.getAngularDragForce(this.angularVelocity,this.angularDrag) * deltaTime;
    }

    getDragForce(velocity, drag) {
        let velMag = velocity.magnitude();
        return -velocity.multiplicationVS(drag).multiplicationVS(velMag);
    }

    getAngularDragForce(angularVelocity, angularDrag) {
        return -angularVelocity * Math.abs(angularVelocity) * angularDrag;
    }

    pushObject(value) {
        this.velocity.additionVS(value);
    }

    turnAround(newAngle) {
        this.angularVelocity + newAngle - this.angle;
    }

    move(angle) {
        this.pushObject(new Vector(Math.cos(angle), Math.sin(angle)).multiplicationVS(this.vitesse));
        this.turnAround(angle);
    }

    teleport(vector) {
        this.position = vector;
    }
}

class Ship extends ObjectPhysics {
    constructor(pos,color) {
        super(pos);
        this.color = color;
        this.life = 3;
    }

    draw(ctx) {
        ctx.save();
        ctx.translate(this.position.x,-this.position.y);
        ctx.rotate(-this.angle);
        ctx.fillStyle = this.color;
        ctx.globalAplha = 1.0;
        ctx.beginPath();
        ctx.moveTo(25,25);
        ctx.lineTo(-25,-25);
        ctx.lineTo(25,-25);
        ctx.lineTo(-25,25);
        ctx.closePath();
        ctx.fill();
        for(var i=0;i<this.life;i++) {
            ctx.fillRect(-25+16*i,40,16,5);
        }
        ctx.restore();
    }

    dropLife() {
        this.life -= 1;
    }
}

class Bullet extends ObjectPhysics {
    constructor(pos) {
        super(pos);
    }

    draw(ctx) {
        ctx.save();
        ctx.translate(this.position.x,-this.position.y);
        ctx.rotate(-this.angle);
        ctx.fillStyle = "black";
        ctx.globalAlpha = 1.0;
        ctx.beginPath();
        ctx.moveTo(5,5);
        ctx.lineTo(-5,5);
        ctx.lineTo(-5,-5);
        ctx.lineTo(5,-5);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    }
}

class BoardV2 extends React.Component {
    constructor(props) {
        super(props);
        this.state = {player: [new Ship(new Vector(200, -100),"blue")], bullet: {}};
        this.physics = new Physics();
    }

    componentDidMount() {
        var array = this.state.player;
        array.push(new Ship(new Vector(100,-200),"blue"));
        this.setState = ({player: array});
        setInterval(this.display(this),1000);
        this.physics.addShip(new Vector(100,-100),"blue");
        this.physics.getShip()[0].teleport(new Vector(200,-200));
        this.physicsInterval = setInterval(() => this.physics.simulate(this.state), 1000/60);
        const ctx = this.canvasRef.getContext("2d");
        this.drawInterval = setInterval(() => this.physics.draw(ctx), 1000/60);
    }

    display(lol) {
        console.log(lol.state);
    }

    componentWillUnmount() {
        window.clearInterval(this.physicsInterval);
        window.clearInterval(this.drawInterval);
    }

    render() {
        return (
            <div>
                <canvas height="600" width="800" ref={ref => this.canvasRef = ref} />
            </div>
        );
    }
}*/

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
            //ctx.canvas.width = window.innerWidth
            //ctx.canvas.height = window.innerHeight
            ctx.clearRect(0, 0, this.canvasRef.width, this.canvasRef.height);
            for(var player in this.props.data.players) {
                var ship = this.props.data.players[player];
                ctx.save();
                ctx.translate(ship.posX,-ship.posY);
                ctx.rotate(-ship.angle*Math.PI/180);
                this.ships(ctx,ship.color,ship.life);
                ctx.restore();

                var score = this.props.data.players[player].score;
                if(this.props.data.bestScore < score) {
                    fetch('/modifS/' + this.props.data.players[player].name + '/' + score, {
                        method: 'POST',
                        header: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: '{}'
                    });
                }

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

    // bouclier (cercle) de protection remplacer par la barre de vie (gestion collision a revoir) )=
    ships = (ctx,fillStyle,life) => {
        ctx.fillStyle = fillStyle;
        ctx.globalAplha = 1.0;
        ctx.beginPath();
        ctx.moveTo(20,0);
        ctx.lineTo(-20,-18);
        ctx.lineTo(-20,18);
        ctx.closePath();
        ctx.fill();
        for(var i=0;i<life;i++) {
            ctx.fillRect(-25+16*i,30,16,5);
        }
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
        this.color = '000000';
    }

    move() {
        if(this.joystickData) {
            if(this.joystickData.distance > 10) {
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

    componentWillMount() {
        for(var player in this.props.data.players) {
            if(this.props.data.players[player].name == this.props.name) {
                this.color = this.props.data.players[player].color;
            }
        }
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
            var p = this.props.data.players[player];
            if(p.name == this.props.name) {
                pName = true;
            }
        }
        if(pName) {
            return (
                <div>
                    <div className="col-xs-6" style={stylesJoystick} id="joystick"></div>
                    <div className="col-xs-6" style={stylesTir} id="tir"></div>
                </div>
            );
        } else {
            document.location.href = "/res/"+ this.props.name +'/'+ this.color.slice(1);
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

export function joystick(node, data, name) {
    ReactDOM.render(<Joystick data={data} name={name} />, node);
}