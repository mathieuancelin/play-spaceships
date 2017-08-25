# Spaceships

## Lancé l'application en dev

##### Play
Dans `/play-spaceships`, lancé les commandes `sbt` :
```
sbt
~run
```

Ensuite la page du jeu est sur l'url `http://localhost:9000`

##### React
En parallèle, dans `/play-spaceships/javascript` avec yarn :
```
yarn install
yarn start
```

## Déploiement
Le déploiement se fait sur `clever cloud`, tant que le problème du script pour build le react n'est pas résolu :
* Build le `bundle.js` de react avec la commande ```yarn run build```

Si le problème est résolu, il faut penser à ajouter la variable d'environement sur clever cloud :
```
BUILD_JS = true
```

## Fonctionnement de l'application
* `/app/controllers/Application` permet de gérer les différents appels du client au serveur (avec les routes définies dans `/conf/routes`)
* `/app/models/Game.scala` contient l'ensemble des classes utilisé pour le jeu
* `/app/state/StateGame.scala` permet la gestion de Akka stream pour gérer l'état de chaque partie
* `/app/views/...`, l'ensemble des templates de play
* `/javascript/src/index.js` contient le code React (3 composants -> affichage du canvas pour une partie, gestion du joystick et affichage de la liste des parties)

## Problème à régler
* Déployer dynamiquement le projet avec Webhook.
* Résoudre le script de déploiement dans `/play-spaceships/projet/javascript.scala`.
* Corriger le débit d'envoie d'évenement du joystick de déplacement.
* Corriger la vélocité et la décélération du vaisseau.

### Piste à voir (des problèmes)
* Le joystick peut parfois se bloquer et envoyer des evenements dans la dernière direction du vaisseau, il faut alors replacer le joystick au centre mais il se bloquera de nouveau au prochain déplacement.
* Modifier les paramètres de vélocité du vaisseau peut résoudre le bug de vitesse. (augmenter le taux de décélération ou diminuer le taux d'accelération par événement).
* Le vaisseau calcul sa vélocité et une décélération, à voir s'il ne faut pas revenir à un déplacement simple (sans gestion de la vélocité).
