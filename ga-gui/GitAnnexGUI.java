import javax.swing.*;
import java.io.*;
import java.util.*;

public class GitAnnexGUI extends JFrame
{
    private ArrayList<AnnexedFile> fileNames;


    public GitAnnexGUI() {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
    }


    /**
     * arg= path iniziale di un primo git-annex, gli altri li evince estraendo info dall'annex stesso
     */
    public static void main(String[] arg) {
        if(arg.length!=1) {
            System.err.println("Missing argument (git-annex path)!");
            System.exit(1);
        }

        File f=new File(arg[0]);
        if(!f.isDirectory()) {
            System.err.println("Argument is not a dir!");
            System.exit(2);
        }

        //TODO: check if it is a git-annex!

        GitAnnexGUI mainWindow=new GitAnnexGUI();
        mainWindow.setVisible(true);

    }
}


/** un singolo file annexed, con la mappa dei remote su cui e' (o si vorrebbe metterlo)
 */
class AnnexedFile extends Observable {
    private File fileName;
    private char[] remotes;
}

class AnnexedVisible extends JTextField //implements Observer
{
    private AnnexedFile file;
}
