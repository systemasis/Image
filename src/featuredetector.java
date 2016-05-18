
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class featuredetector {

public static String detection(String inFile){
	
	
	String path;
	StringBuffer sb;
	StringBuffer sbc = new StringBuffer();
	char tmp = 0;
	char numero = 0;
	char couleur = 0;
	int maxscore = 0;
	

	//parcours toute les cartes possibilités
	for(int i=1;i<=13;i++){
		
		sb = new StringBuffer(i);
		
		if(i<=10 && i>=1){
		}
		else if (i==1){
			tmp = 'A';
		}
		else if (i==11){
			tmp = 'V';
		}
		else if(i==12){
			tmp = 'Q';
		}
		else if(i==13){
			tmp='R';
		}
		sb.append(tmp);
		sb.append(".jpg");
		path = sb.toString();
		
		if(match(inFile, path , "resultat.jpg", i)>=maxscore){
			numero = tmp;
		}
	}
	
	for(int i=0;i<4;i++){
		sb = new StringBuffer();
		switch(i){
		case  0 : tmp = 't';
		
		break;
		case  1 : tmp = 'p';
		break;
		case 2 : tmp = 'd';
		break;
		case 3 : tmp = 'c';
		break;
		}
		sb.append(tmp);
		sb.append(".png");
		path = sb.toString();
		if(match(inFile, path, "resultat.jpg", i)==1){
			couleur = tmp;
		}
		}
	sbc.append(numero + " " + couleur );
	return sbc.toString();
}

	public static int match(String inFile, String templateFile, String outFile, int match_method) {
    	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("\nRunning Template Matching");

        Mat img = Highgui.imread(inFile);
        Mat templ = Highgui.imread(templateFile);

        // / Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(img, templ, result, match_method);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        
        // / Localizing the best match with minMaxLoc
        System.out.println(result.total()/(result.height()*result.width()));
        
        MinMaxLocResult mmr = Core.minMaxLoc(result);
        
        System.out.println(result.total()/(result.height()*result.width()));
        

        Point matchLoc;
        
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        
        } else {
            matchLoc = mmr.maxLoc;
            
        }
        

        return (int) mmr.maxVal;
	}
	
	public static void main (String [] Args){
		System.out.println(detection("cheminimage"));
	}
	
	}
	

