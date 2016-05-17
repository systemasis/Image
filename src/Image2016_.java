import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ij.*;
import ij.gui.*;
import ij.io.FileSaver;
import ij.plugin.filter.*;
import ij.process.*;

public class Image2016_ implements PlugInFilter {

	private String FILEPATH = "D:/Shank/Documents/code/Java/ImageJ/buffer.txt";
	private BufferedWriter BW;
	private String TITRE = "Image2016";
	private boolean TEST = true;

	public void run(ImageProcessor ip) {

		try{
			if(this.TEST)
				this.BW = new BufferedWriter(new FileWriter(new File(this.FILEPATH)));

			// Filtre Gaussien
			ImagePlus imp = this.gauss(ip);
			ip = imp.getProcessor();

			if(this.TEST){
				this.BW.write("Avant Sobel");
				this.BW.newLine();
			}

			// Étirement
			this.etirement(ip);

			// Filtre moyennant
			this.filtreMoyennant(ip);

			// Sobel
			this.sobel(ip);

			if(this.TEST){
				this.BW.write("Après Sobel");
				this.BW.newLine();
			}

			this.binarisation(ip, 50);

			// Squelletisation
			// int mat[] = {
			// 255, 255, 255,
			// 5, 0, 255,
			// 0, 0, 5
			// };
			// imp = this.erosion(ip, mat);

			// Composantes Connexes
			HashMap<Integer, ArrayList<int[]>> composantes = this.getComposantes(ip);

			if(this.TEST){
				this.BW.write("Nombre de composantes = " + composantes.size());
				this.BW.newLine();
			}

			this.filterComposantes(composantes);

			if(this.TEST){
				this.BW.write("Nombre de composantes = " + composantes.size());
				this.BW.newLine();
			}

			// TODO Ajouter l'étude des composantes connexes
			FileSaver fl = new FileSaver(imp);
			fl.saveAsJpeg("test.jpeg");

			File file = new File("test.jpeg");
			file.delete();

			if(this.TEST){
				this.BW.close();
			}

			new ImageWindow(imp);

		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int setup(String args, ImagePlus imp) {
		return NO_CHANGES + DOES_8G;
	}
	
	/**
	 * /!\ Ne fonctionne pas correctement /!\
	 * Érode une image selon la matrice mat
	 * 
	 * @param ImageProcessor
	 *            ip
	 * @param int[]
	 *            mat
	 * @return ImagePlus l'image érodée
	 * @throws IOException
	 */
	private ImagePlus erosion(ImageProcessor ip, int[] mat) throws IOException {
		boolean erodable;
		int y, x = 1, width = ip.getWidth(), height = ip.getHeight();
		ImagePlus img = NewImage.createByteImage(this.TITRE, ip.getWidth(), ip.getHeight(), 1, NewImage.FILL_BLACK);
		ImageProcessor ip2 = img.getProcessor();

		/* variables de tests */
		int boucles = 0, erode = 0, bienerode = 0;
		boolean aeroder = false;
		
		do{
			erode = 0;
			while(x < width){
				y = 1;
				while(y < height){
					erodable = true;
					// Création de la matrice du pixel sélectionné et ses voisins
					int[] pixels = {
							ip.getPixel(x - 1, y - 1), // bottomGauche
							ip.getPixel(x - 1, y), // middleGauche
							ip.getPixel(x - 1, y + 1), // topGauche
							ip.getPixel(x, y - 1), // bottomMiddle
							ip.getPixel(x, y), // middleMiddle
							ip.getPixel(x, y + 1), // topMiddle
							ip.getPixel(x + 1, y - 1), // bottomDroit
							ip.getPixel(x + 1, y), // middleDroit
							ip.getPixel(x + 1, y + 1) // topDroit
					};

					int i = 0;

					while(erodable && i < 9){
						if(mat[i] == 255 || mat[i] == 0){
							if(pixels[i] != mat[i])
								erodable = false;
						}
						i++;
					}

					if(erodable){
						ip2.putPixel(x, y, 0);
					}else{
						if(this.TEST && ip.getPixel(x, y) == 0){
							erode++;
							aeroder = true;
						}
						
						ip2.putPixel(x, y, 255);
						
						if(this.TEST && ip2.getPixel(x, y) == 255 && aeroder){
							bienerode++;
						}

						aeroder = false;
					}

					y++;
				}
				x++;
			}
			// On ne travaille plus qu'avec la nouvelle image
			ip = ip2;
			boucles++;

			if(this.TEST){
				this.BW.write("Nombre de boucles : " + boucles + "; Nombre d'érosion : " + erode + "; Bien érodés = " + bienerode);
				this.BW.newLine();
			}
		}while(erode > 0 && boucles < 100);

		return img;
	}

	/**
	 * Supprime les composantes excessivements petites ou excessivements grosses
	 * 
	 * @param composantes
	 */
	private void filterComposantes(HashMap<Integer, ArrayList<int[]>> composantes) {
		for(int i = 0; i < composantes.size(); i++){
			if(composantes.get(i).size() < 5 || composantes.get(i).size() > 10000){
				composantes.remove(i);
				removeComposanteAndReset(composantes, i);
			}
		}
	}

	/**
	 * Réduit le bruit au moyen d'un matrice 3x3 [1, 2, 1, 2, 4, 2, 1, 2, 1]
	 * 
	 * @param ip
	 */
	private void filtreMoyennant(ImageProcessor ip) {
		int height = ip.getHeight(), width = ip.getWidth(), moyenne;

		for(int x = 1; x < width - 1; x++){
			for(int y = 1; y < height - 1; y++){
				moyenne = 0;

				for(int x2 = x - 1; x2 < x + 2; x2++){
					for(int y2 = y - 1; y2 < y + 2; y2++){
						// Au-dessus ou à côté du pixel
						if((x2 == x && (y2 == y - 1 || y2 == y + 1)) || ((x2 == x - 1 || x2 == x + 1) && y2 == y))
							moyenne += 2 * ip.getPixel(x2, y2);
						else if(x2 == x && y2 == y) // Le pixel
							moyenne += 4 * ip.getPixel(x2, y2);
						else // Les coins
							moyenne += ip.getPixel(x2, y2);
					}
				}
				moyenne /= 16;
				ip.putPixel(x, y, moyenne);
			}
		}
	}

	/**
	 * Binarise l'image en mettant en blanc les pixels supérieur au seuil et noir
	 * ceux inférieurs ou égaux au seuil
	 * 
	 * @param ip
	 * @return
	 */
	private void binarisation(ImageProcessor ip, int seuil) {
		int width = ip.getWidth(), height = ip.getHeight(), color;

		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				color = ip.getPixel(x, y);
				if(color <= seuil)
					ip.putPixel(x, y, 255);
				else
					ip.putPixel(x, y, 0);
			}
		}
	}

	/**
	 * Supprime une entrée et comble le vide par celles qui suivent.
	 * 
	 * @param composantes
	 * @param k
	 */
	private void removeComposanteAndReset(HashMap<Integer, ArrayList<int[]>> composantes, int k) {
		for(; k < composantes.size() - 1; k++){
			composantes.put(k, composantes.get(k + 1));
		}

		composantes.remove(k);
	}

	/**
	 * Permet d'étirer l'histogramme et d'obtenir une image parfois plus claire ou nette
	 * 
	 * @param ip
	 */
	private void etirement(ImageProcessor ip) {
		// On veut que le niveau de gris le plus bas soit 0 et le plus haut 255.
		// la fonction de transformation est la suivante:
		// y = (255 * (x - min)) / (max - min)
		// oÃ¹:
		// x est le niveau de gris avant transformation
		// y est le niveau de gris aprÃ¨s transformation
		// min est le plus petit niveau de gris avant transformation
		// max est le plus grand niveau de gris avant transformation*
		int min = 255;
		int max = 0;
		for(int i = 0; i < ip.getHeight(); i++){
			for(int j = 0; j < ip.getWidth(); j++){
				int pixel = ip.getPixel(j, i);
				if(pixel > max)
					max = pixel;
				if(pixel < min)
					min = pixel;
			}
		}

		for(int i = 0; i < ip.getHeight(); i++){
			for(int j = 0; j < ip.getWidth(); j++){
				int pixel = ip.getPixel(j, i);
				ip.putPixel(j, i, (255 * (pixel - min)) / (max - min));
			}
		}
	}

	/**
	 * Filtre de gausse
	 * 
	 * @param ip
	 * @return
	 * @throws IOException
	 */
	private ImagePlus gauss(ImageProcessor ip) throws IOException {
		// rayon: rayon du masque gaussien
		int rayon = 1;

		// pour avoir une "cloche" a peu pres complete,
		// on choisit un ecart-type egal a un tiers du rayon.
		double sigma = rayon / 3.0;

		// mat: une matrice pour stocker le resultat de la convolution.
		double[][] mat = null;

		Masque gaussienne = new Masque(rayon);

		// Calcul du masque gaussien
		double somme = 0;
		for(int i = -rayon; i <= rayon; i++){
			for(int j = -rayon; j <= rayon; j++){
				double d = Math.exp(-(j * j + i * i) / (2 * sigma * sigma));
				d /= 2 * Math.PI * sigma * sigma;
				gaussienne.put(j, i, d);
				somme += d;
			}
		}

		// Normalisation du masque
		for(int i = -rayon; i <= rayon; i++){
			for(int j = -rayon; j <= rayon; j++){
				gaussienne.put(j, i, gaussienne.get(j, i) / somme);
			}
		}

		mat = this.convoluer(ip, gaussienne);

		ImagePlus imp = NewImage.createByteImage(this.TITRE, ip.getWidth(), ip.getHeight(), 1, NewImage.FILL_BLACK);
		ip = imp.getProcessor();

		// affichage de la matrice
		if(mat != null)
			this.appliquerMatrice(ip, mat, false);

		return imp;
	}

	/**
	 * Cherches les composantes connexes d'une image
	 * 
	 * @param ip
	 *            L'image à étudier
	 * @return composantes Les composantes connexes trouvées sous la forme suivante HashMap( index =>
	 *         ArrayList(Integer[x, y]) )
	 * 
	 *         index étant le numéro de la composante partant de 0 et s'incrémentant de 1 à chaque composante ajoutée x
	 *         et y étant les coordonnées respectivement verticales et horizontales d'un pixel
	 */
	private HashMap<Integer, ArrayList<int[]>> getComposantes(ImageProcessor ip) {

		// Numéro de la composante -> liste des pixels qu'elle inclut
		HashMap<Integer, ArrayList<int[]>> composantes = new HashMap<Integer, ArrayList<int[]>>();
		int height = ip.getHeight(), width = ip.getWidth();

		// Parcours des pixels
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){

				ArrayList<int[]> toAdd = new ArrayList<int[]>();
				int composanteKey = -1;

				// Si le pixel est d'une couleur au-dessus du seuil (donc qu'il fait partit du contour)
				if(ip.getPixel(x, y) == 0){

					// s'il n'est pas dans les composantes on l'indique comme étant à ajouter, autrement on prend la
					// clé de sa composante
					if(!this.isVisited(composantes, x, y)){
						int[] pixel = {
								x, y
						};
						toAdd.add(pixel);
					}else{
						composanteKey = this.getComposanteKey(composantes, x, y);
					}

					/*
					 * On vérifie si les pixels autour sont dans les bonnes couleurs et s'ils font déjà parti d'une
					 * composante Voisine
					 * 
					 * Dans une composante ? Si le pixel sélectionné n'a pas de composante on le place dans celle-ci
					 * autrement on ajoute celle du voisin dans la composante provisoire du pixel sélectionné
					 * 
					 * Voisin pas dans une composante ? On passe au suivant
					 */
					for(int j = y - 1; j < y + 2; j++){
						for(int i = x - 1; i < x + 2; i++){
							// Le pixel voisin est dans les bonnes couleurs
							if(i != x && j != y && ip.getPixel(i, j) == 0){

								// Si le voisin du pixel sélectionné fait parti d'une composante
								if(this.isVisited(composantes, i, j)){
									// Si le pixel sélectionné ne fait pas parti d'unr composante, on le place dans
									// celle du voisin
									if(composanteKey == -1){
										composanteKey = this.getComposanteKey(composantes, i, j);
									}
									// Si le pixel sélectionné a été ajouté à une composante et qu'un voisin fait
									// déjà parti d'une autre composante c'est que plusieurs se rejoignent : une
									// fusion
									// s'impose
									else if(composanteKey != this.getComposanteKey(composantes, i, j)){
										int composanteKey2 = this.getComposanteKey(composantes, i, j);
										ArrayList<int[]> insertInto = composantes.get(composanteKey);
										Iterator<int[]> iterator = composantes.get(composanteKey2).iterator();

										while(iterator.hasNext()){
											insertInto.add(iterator.next());
										}

										composantes.put(composanteKey, insertInto);

										this.removeComposanteAndReset(composantes, composanteKey2);
									}
								}else{ // Il n'en fait pas parti, donc on le rajoute à la composante en devenir
									int[] pixel2 = {
											i, j
									};
									toAdd.add(pixel2);
								}

								// Si aucune composante n'a été sélectionnée pendant l'exploration des voisins, on
								// place la composante provisoire à la fin
								if(composanteKey == -1)
									composanteKey = composantes.size();

								Iterator<int[]> iterator = toAdd.iterator();
								ArrayList<int[]> insertInto = composantes.get(composanteKey);

								if(insertInto == null)
									insertInto = new ArrayList<int[]>();

								// On ajoute les pixels découverts dans la composante correspondantes
								while(iterator.hasNext()){
									insertInto.add(iterator.next());
								}

								composantes.put(composanteKey, insertInto);
							}
						}
					}
				}
			}
		}

		return composantes;
	}

	/**
	 * Retourne la clé dans le hashmap composantes de la composante contenant le pixel aux coordonnées x et y
	 * 
	 * @param composantes
	 * @param x
	 * @param y
	 * @return int key le numéro de la composante ou -1 si aucune composante ne contient le pixel
	 */
	private int getComposanteKey(HashMap<Integer, ArrayList<int[]>> composantes, int x, int y) {
		int key = -1, i = 0;

		while(key < 0 && i < composantes.size()){
			Iterator<int[]> iterator = composantes.get(i).iterator();
			while(iterator.hasNext()){
				int[] next = iterator.next();
				if(next[0] == x && next[1] == y)
					key = i;
			}
			i++;
		}

		return key;
	}

	/**
	 * Vérifie que le pixel aux coordonnées x et y ne soient pas déjà présent dans composantes
	 * 
	 * @param composantes
	 * @param x
	 * @param y
	 * @return boolean visited true s'il est présent et false autrement
	 */
	private boolean isVisited(HashMap<Integer, ArrayList<int[]>> composantes, int x, int y) {
		boolean visited = false;
		int i = 0;

		while(i < composantes.size() && !visited){
			Iterator<int[]> iterator = composantes.get(i).iterator();

			while(iterator.hasNext() && !visited){
				int[] next = iterator.next();
				if(next[0] == x && next[1] == y)
					visited = true;
			}
			i++;
		}

		return visited;
	}

	/**
	 * Effectue la convolution de l'image 'ip' avec un masque carre. Le resultat d'un produit de convolution n'est pas
	 * forcement dans le meme domaine de definition que l'image d'origine. C'est pourquoi le resultat est stocke dans
	 * une matrice de nombres reels.
	 * 
	 * @param ip
	 *            L'image a convoluer.
	 * @param masque
	 *            Le masque de convolution.
	 * @return La matrice resultat.
	 */
	private double[][] convoluer(ImageProcessor ip, Masque masque) {
		// resultat: la matrice dans laquelle sera stocke le resultat de la convolution.
		double[][] resultat = new double[ip.getWidth()][ip.getHeight()];

		int rayon = masque.getRayon();
		for(int i = rayon; i < ip.getHeight() - rayon; i++){
			for(int j = rayon; j < ip.getWidth() - rayon; j++){
				resultat[j][i] = 0;
				// Parcours du masque de convolution
				for(int v = -rayon; v <= rayon; v++){
					for(int u = -rayon; u <= rayon; u++){
						resultat[j][i] += masque.get(u, v) * ip.getPixel(j - u, i - v);
					}
				}
			}
		}
		return resultat;
	}

	/**
	 * Filtre de Sobel
	 * 
	 * @param ip
	 * @throws IOException
	 */
	private void sobel(ImageProcessor ip) throws IOException {
		// mat: la matrice a remplir
		double[][] mat;

		Masque sobelX = new Masque(1);
		sobelX.put(-1, -1, -1);
		sobelX.put(1, -1, 1);
		sobelX.put(-1, 0, -2);
		sobelX.put(1, 0, 2);
		sobelX.put(-1, 1, -1);
		sobelX.put(1, 1, 1);
		double[][] matX = this.convoluer(ip, sobelX);

		Masque sobelY = new Masque(1);
		sobelY.put(-1, -1, -1);
		sobelY.put(0, -1, -2);
		sobelY.put(1, -1, -1);
		sobelY.put(-1, 1, 1);
		sobelY.put(0, 1, 2);
		sobelY.put(1, 1, 1);
		double[][] matY = this.convoluer(ip, sobelY);

		mat = new double[ip.getWidth()][ip.getHeight()];
		for(int i = 0; i < ip.getHeight(); i++){
			for(int j = 0; j < ip.getWidth(); j++){
				mat[j][i] = Math.sqrt(Math.pow(matX[j][i], 2) + Math.pow(matY[j][i], 2));
			}
		}

		this.appliquerMatrice(ip, mat, true);

	}

	/**
	 * Applique une matrice à une image
	 * 
	 * @param ip
	 * @param mat
	 * @param normaliser
	 * @throws IOException
	 */
	private void appliquerMatrice(ImageProcessor ip, double[][] mat, boolean normaliser) throws IOException {
		if(this.TEST){
			this.BW.write("Avant Normalisation");
			this.BW.newLine();
		}

		if(normaliser){
			double max = mat[0][0];
			double min = mat[0][0];
			for(int y = 0; y < mat[0].length; y++){
				for(int x = 0; x < mat.length; x++){
					if(mat[x][y] > max)
						max = mat[x][y];
					if(mat[x][y] < min)
						min = mat[x][y];
				}
			}

			if(min != max){
				for(int y = 0; y < mat[0].length; y++){
					for(int x = 0; x < mat.length; x++){
						ip.putPixel(x, y, (int) ((255 * (mat[x][y] - min)) / (max - min)));
					}
				}
			}
		}

		else{
			for(int y = 0; y < mat[0].length; y++){
				for(int x = 0; x < mat.length; x++){
					int p = (int) Math.min(mat[x][y], 255);
					p = Math.max(p, 0);
					ip.putPixel(x, y, p);
				}
			}
		}
	}

	/**
	 * Retourne le plus petit de a et b
	 * 
	 * @param a
	 * @param b
	 * @return le plus petit nombre de a et b
	 */
	public static int min(int a, int b) {
		if(a > b)
			return b;
		else
			return a;
	}

	/**
	 * Retourne le plus grand de a et b
	 * 
	 * @param a
	 * @param b
	 * @return le plus grand nombre de a et b
	 */
	public static int max(int a, int b) {
		if(a < b)
			return b;
		else
			return a;
	}

	public class Masque {

		private double[] contenu;
		private int rayon;
		private int largeur;

		/**
		 * Cree un nouveau masque de convolution. C'est un carre de cote (2 * rayon + 1). Tous les elements sont a zero.
		 * 
		 * @param rayon
		 *            Rayon du masque de convolution.
		 */
		public Masque(int rayon) {
			this(rayon, 0);
		}

		/**
		 * Cree un nouveau masque de convolution. C'est un carre de cote (2 * rayon + 1). Tous les elements sont a
		 * 'valeurParDefaut'.
		 * 
		 * @param rayon
		 *            Rayon du masque de convolution.
		 * @param valeurParDefaut
		 *            Valeur a mettre dans chaque element.
		 */
		public Masque(int rayon, double valeurParDefaut) {
			if(rayon < 1){
				throw new IllegalArgumentException("Le rayon doit etre >= 1");
			}

			this.rayon = rayon;
			largeur = 2 * rayon + 1;
			contenu = new double[largeur * largeur];

			for(int i = 0; i < largeur * largeur; i++){
				contenu[i] = valeurParDefaut;
			}
		}

		/**
		 * Renvoie le rayon (demie-largeur) du masque.
		 * 
		 * @return Le rayon.
		 */
		public int getRayon() {
			return rayon;
		}

		/**
		 * Renvoie la largeur du masque (cote du carre).
		 * 
		 * @return La largeur.
		 */
		public int getLargeur() {
			return largeur;
		}

		/**
		 * Remplit le masque avec la valeur passee en argument.
		 * 
		 * @param valeur
		 *            Valeur a stocker dans chaque element.
		 */
		public void remplirAvec(double valeur) {
			for(int i = 0; i < largeur * largeur; i++){
				contenu[i] = valeur;
			}
		}

		/**
		 * Renvoie un element du masque.
		 * 
		 * @param x
		 *            Abscisse de l'element.
		 * @param y
		 *            Ordonnee de l'element.
		 * @return La valeur contenue dans l'element de coordonnees (x,y).
		 */
		public double get(int x, int y) {
			return contenu[(y + rayon) * largeur + x + rayon];
		}

		/**
		 * Modifie un element du masque.
		 * 
		 * @param x
		 *            Abscisse de l'element.
		 * @param y
		 *            Ordonnee de l'element.
		 * @param valeur
		 *            Valeur a inscrire dans l'element de coordonnees (x,y).
		 */
		public void put(int x, int y, double valeur) {
			contenu[(y + rayon) * largeur + x + rayon] = valeur;
		}

	}
}
