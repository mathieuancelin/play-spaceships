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
                ctx.rotate(-s.angle*Math.PI/180);
                this.ships(ctx,"#"+s.color,s.life);
                ctx.restore();
            }
            for(let bullet in this.props.data.bullets) {
                let b = this.props.data.bullets[bullet];
                ctx.save();
                ctx.translate(b.posX, -b.posY);
                ctx.rotate(-b.angle*Math.PI/180);
                this.bullet(ctx);
                ctx.restore();
            }
            ctx.font = "20px Arial";
            let text = this.props.data.nameScore + " - " + this.props.data.bestScore
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
        let opacityShield = 0;
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
            color: "blue"
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
            color: "blue"
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

/*
 * PARTY MANAGER
 * Handle different game for spaceships
 */

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
            <form onSubmit={this.onSubmit.bind(this)}>
                <input value={this.state.input} onChange={this.onChange} />
                <button>Create game</button>
            </form>
        </div>);
    }
}

const List = props => (
    <table>
        <tr>
            <th>Room name</th>
            <th>Views</th>
            <th>Play</th>
        </tr>
        {
            props.items.map((item, index) =>
                <tr key={index}>
                    <td> <Glyphicon glyph="star" /> {item}</td>
                    <td><a href={"/board/"+index.toString()}>View</a></td>
                    <td><a href={"/m/"+index.toString()}>Play</a></td>
                </tr>)
        }
    </table>
);

export function partyManager(node, gameList, host) {
    ReactDOM.render(<GameInstance gameList={gameList} host={host} />, node);
}