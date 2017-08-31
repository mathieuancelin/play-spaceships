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
            for(let ship in this.props.data.ships) {
                let s = this.props.data.ships[ship];
                ctx.save();
                ctx.translate(s.posX,-s.posY);
                ctx.rotate((90 - s.angle)*Math.PI/180);
                this.ships(ctx,"#"+s.color,s.life);
                ctx.restore();
            }
            for(let bullet in this.props.data.bullets) {
                let b = this.props.data.bullets[bullet];
                ctx.save();
                ctx.translate(b.posX, -b.posY);
                ctx.rotate((90 - b.angle)*Math.PI/180);
                this.bullet(ctx);
                ctx.restore();
            }
            ctx.font = "20px Permanent Marker";
            let text = this.props.data.nameScore + " - " + this.props.data.bestScore;
            ctx.fillStyle = "#f4d533";
            ctx.fillText(text,800-40*this.props.data.nameScore.length,30);
        }
        setTimeout(this.draw, 100);
    }

    ships = (ctx,fillStyle,life) => {
        ctx.fillStyle = fillStyle;
        ctx.globalAplha = 1.0;
        ctx.beginPath();
        // ctx.moveTo(20,0);
        // ctx.lineTo(-20,-18);
        // ctx.lineTo(-20,18);
        ctx.arc(-2, 1, 5, 0, 2 * Math.PI);
        var imageShip = new Image();
        imageShip.src = '/assets/images/spaceship.png'
        ctx.drawImage(imageShip,-27,-33,50,85);
        ctx.closePath();
        ctx.fill();
        let opacityShield = 0;
        switch(life) {
            case 3 : opacityShield = 0.15;break;
            case 2 : opacityShield = 0.08;break;
            case 1 : opacityShield = 0;break;
        }
        ctx.fillStyle="rgba(23,145,167,"+opacityShield+")";
        ctx.arc(-3,9,60,0,Math.PI*2,true);
        ctx.fill();
    }

    bullet = (ctx) => {
        ctx.fillStyle = "#77c9ff";
        ctx.globalAlpha = 1.0;
        ctx.beginPath();
        // ctx.moveTo(8,1.5);
        // ctx.lineTo(-8,1.5);
        // ctx.lineTo(-8,-1.5);
        // ctx.lineTo(8,-1.5);

        var imageBombe = new Image();
        imageBombe.src = '/assets/images/bombe.png'
        ctx.drawImage(imageBombe,-6,-30,10,34);

        ctx.closePath();
        ctx.fill();
    }

    render() {
        let rows = [];
        if(this.props.data) {
            let dataJ = JSON.stringify(this.props.data);

            // Opti possible ???
            for(let player in this.props.data.players) {
                rows.push(this.props.data.players[player].name+": "+this.props.data.players[player].score);
            }
        }
        return (
            <div>
              <canvas height="600" width="1000" ref={ref => this.canvasRef = ref} />
              <Leaderboard rows={rows}/>
            </div>
        );
    }
}

const stylesJoystick = {
    backgroundImage: "url("+"/assets/images/joystick.png"+")",
    backgroundSize: "contain",
    backgroundPosition: "center",
    backgroundRepeat: "no-repeat",
    height: '120px'
};
const stylesTir = {
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
        this.wsCoonectGame = new WebSocket("ws://"+this.props.host+"/wsG/"+this.props.id);
        this.wsCoonectGame.onclose = evt => {
            document.location.href = "/res/"+ this.id + "/" + this.name +'/'+ this.color;
        };
    }

    move() {
        if(this.joystickData) {
            if(this.joystickData.distance > 10) {
                let js = JSON.stringify({"action": "moveShip","id": this.props.ship.id.toString(), "angle": this.joystickData.angle.radian.toString()});
                this.wsCoonectGame.send(js);
            }
        }
    }

    shoot() {
        let js = JSON.stringify({"action": "addBullet", "id": this.props.ship.id.toString()});
        this.wsCoonectGame.send(js);
    }

    componentWillMount() {
        this.id = this.props.id;
        this.name = this.props.ship.name;
        this.color = this.props.ship.color;
        document.body.addEventListener('keyup', this.handleInput.bind(this), false);
    }

    handleInput(e) {
        if(event.keyCode == '32') {
            this.shoot();
        }
    }

    componentDidMount() {
        let that = this;

        // ZONE DE CONTROLE
        const joystickParams = {
            zone: document.getElementById("joystick"),
            color: "#484747"
        };
        let manager = njs.create(joystickParams);
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
            color: "#484747"
        };
        const manager2 = njs.create(tirParams);
        manager2.on('added', function(evt, nipple) {
            that.shoot();
        });
    }

    render() {
        if(this.props.ship) {
            return (
                <div>
                    <div className="col-xs-6" style={stylesJoystick} id="joystick" ></div>
                    <div className="col-xs-6" style={stylesTir} id="tir"></div>
                </div>
            );
        } else {
            this.wsCoonectGame.onclose();
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

export function board(node, data) {
    if(data == null) {
        ReactDOM.render(<Board />,node);
    }
  ReactDOM.render(<Board data={data} />, node);
}

export function joystick(node, id, data, ship, host) {
    ReactDOM.render(<Joystick data={data} ship={ship} host={host} id={id} />, node);
}


class GameInstance extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            input: '',
            items: this.props.gameList
        }
        this.wsCreateGame = new WebSocket("ws://"+this.props.host+"/wsGl");
        this.wsCreateGame.onmessage = evt => {
            this.wsCreateGame.close();
            document.location.href = "/board/"+evt.data;
        };
    }

    onChange = (event) => {
        this.setState({input: event.target.value});
    }

    onSubmit(event) {
        event.preventDefault();
        let js = JSON.stringify({"name": this.state.input});
        this.wsCreateGame.send(js);
        this.setState({
            input: '',
            items: [...this.state.items, this.state.input]
        });
    }

    render() {
        return(<div>
            <List items={this.state.items} />
            <br />
            <form onSubmit={this.onSubmit.bind(this)}>
                <input value={this.state.input} onChange={this.onChange} />
                <button>Create game</button>
            </form>
        </div>);
    }
}

const List = props => (
    <table>
        <thead>
            <tr>
                <th>Room name</th>
                <th>Views</th>
                <th>Play</th>
            </tr>
        </thead>
        <tbody>
        {
            props.items.map((item, index) =>
                <tr key={index}>
                    <td><span className="glyphicon glyphicon-chevron-right"></span>{item}</td>
                    <td><a href={"/board/"+index.toString()}><span className="glyphicon glyphicon-eye-open">View</span></a></td>
                    <td><a href={"/m/"+index.toString()}><span className="glyphicon glyphicon-knight">Play</span></a></td>
                </tr>)
        }
        </tbody>
    </table>
);

export function partyManager(node, gameList, host) {
    ReactDOM.render(<GameInstance gameList={gameList} host={host} />, node);
}
