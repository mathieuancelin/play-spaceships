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

## Architecture de l'application
* `/app/controllers/Application` permet de gérer les différents appels du client au serveur (avec les routes définies dans `/conf/routes`)
* `/app/models/Game.scala` contient l'ensemble des classes utilisé pour le jeu
* `/app/state/StateGame.scala` permet la gestion de Akka stream pour gérer l'état de chaque partie
* `/app/views/...`, l'ensemble des templates de play
* `/javascript/src/index.js` contient le React (3 composants -> affichage du canvas pour une partie, gestion du joystick et affichage de la liste des parties)

##### Indication
- Le canvas se trouve dans le composant react `Board`
- Le joystick se trouve dans le composant react `Joystick`
- Le tableau d'affichage des parties se trouve dans le composant react `GameInstance`

Pour le canvas la fonction `draw` s'occupe de dessiner à chaque frame l'ensemble des vaisseaux et des tirs. C'est les fonctions `ships` et `bullet` qui s'occupe de dessiner un modèle à l'endroit indiqué par la fonction `draw`. La fonction `ship` s'occupe de gérer le tracer sur trois points (pour former le triangle) et le bouclier en fonction du nombre de vie du vaisseau.


## Problème à régler
* Déployer dynamiquement le projet avec Webhook.
* Résoudre le script de déploiement dans `/play-spaceships/projet/javascript.scala`.
* Corriger la vitesse de déplacement des vaisseaux de plus en plus élevés en fonction du nombre d'action des vaisseaux sur le terrain.
* Corriger les problèmes du joystick de déplacement qui peut parfois se bloquer dans une direction (même quand on relache).

### Piste à voir (des problèmes)
* L'accélération du vaisseau peut venir des 'ticks' d'Akka Stream qui appel directement la méthode qui simule le vaisseau. Il peut aussi être causé par le nombre plus important d'envoie d'évenement par le joystick (qui incrémente continuellement l'accélération) par rapport à la décélération qui ne suit pas.
* Modifier la facon dont est géré le joystick (nipple.js) ou trouver une meilleure librairie.
* Modifier les paramètres de vélocité du vaisseau, peut résoudre le bug de vitesse. (augmenter le taux de décélération ou diminuer le taux d'accelération par événement).
* Le vaisseau calcul sa vélocité et une décélération, à voir s'il ne faut pas revenir à un déplacement simple (sans gestion de la vélocité).
