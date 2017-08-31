# Spaceships

## Lancé l'application en dev

### Prérequis

1. Installer la dernière version de 'node'
2. Installer la dernière version de 'sbt'
3. Installer la dernière version de 'yarn'
4. Télécharger java 8 et définir la variable d'environnement JAVA_HOME sur le path du JDK

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

## Fonctionnalité à ajouter
* Suppression d'une partie à partir de la liste des parties.
* Ajouter la génération d'un QRCode pour rejoindre chaque parties.
* Catch les erreurs de page non trouvé.


## Problème à régler
* Déployer dynamiquement le projet avec Webhook.
* Résoudre le script de déploiement dans `/play-spaceships/projet/javascript.scala`.
* Corriger le bug du joystick (détaillé ci-dessous).

### Détails "bug joystick"
* Le joystick peut parfois (rarement) se bloquer et garder le setInterval ouvert. Lorsqu'on déplace à nouveau le joystick on obtient du interval qui envoie des données ce qui double la vitesse du vaisseau (peut se bloquer plusieurs fois).
###### Piste:
* Voir s'il y a une autre façon de gérer le joystick (rajout d'un boolean, remplacer le setInterval).
* Changer de librairie pour le joystick (actuellement nipple.js).