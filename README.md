# image
Reconnaissance de carte pour l'UE Traitement d'Image

Ceci est un plugin pour le programme de manipulation d'image ImageJ, créé dans le cadre de l'UE Traitement Image de l'université Paris Descartes.

#Dependencies

Selon que vous soyez sur Windows ou Unix, il vous faudra installer openCV.
Dans tous les cas, téléchargez la version 2.4.12 d'openCV (ici)[http://opencv.org/downloads.html] pour votre système d'exploitation.

###Windows

Une fois les fichiers extraits, copiez le fichiers {dossier d'extraction}\build\java\{x64|x86}\opencv_java2412.dll dans le dossier src/ du projet et ajoutez-le au Build Path.

###Unix

Que ce soit Mac ou Linux, suivez le guide d'installation (à cette adresse)[http://docs.opencv.org/2.4/doc/tutorials/introduction/linux_install/linux_install.html#linux-installation]

###Dernière étape

Afin de pouvoir utilisé les bibliothèques que nous venons de télécharger ainsi que celle d'ImageJ, présente dans le dossier src/ du projet, il est important de les renseigner dans le Build Path.

#Building

Dans votre espace de travail, entrez les lignes de commandes suivantes :

```
git clone https://github.com/systemasis/image
cd image/src
javac -cp "../src/*" Image2016_.java
```

#Importer

Une fois les .class créés, copiez-les ou déplacez-les dans le dossier "plugins" d'ImageJ ou importantez-les un à un via le programme.

#Utilisation

Ouvrez une image avec ImageJ puis sélectionnez le plugin "Image2016". Une fenêtre avec la(les) carte(s) trouvée(s) devrait apparaître après quelques secondes.
