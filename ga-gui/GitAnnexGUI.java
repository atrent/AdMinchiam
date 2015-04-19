import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.*;

public class GitAnnexGUI extends JFrame {
    private File originDir;
    private ArrayList<AnnexedFile> annexedFiles;

    public GitAnnexGUI(File f) {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);

        // TODO: usare JList!!!
        //add(new JList(...));

        initFromAnnex(f);
    }

    public void initFromAnnex(File f) {
        //TODO: check if it is a git-annex!

        Process process=Runtime.getRuntime().exec("git-annex list",null,f);
        InputStream err=process.getErrorStream();
        InputStream out=process.getInputStream();

        while(process.isAlive()) {
            while(err.available()>0)System.err.print((char)err.read());
            //while(out.available()>0)System.out.print((char)out.read());
        }

    }

    /**
     * arg= path iniziale di un primo git-annex, gli altri li evince estraendo info dall'annex stesso
     */
    public static void main(String[] arg) throws Throwable {
        if(arg.length!=1) {
            System.err.println("Missing argument (git-annex path)!");
            System.exit(1);
        }

        File f=new File(arg[0]);
        if(!f.isDirectory()) {
            System.err.println("Argument is not a dir!");
            System.exit(2);
        }

        GitAnnexGUI mainWindow=new GitAnnexGUI(f);
        mainWindow.setVisible(true);

        //TODO: riempire da 'git-annex list'
    }
}

/** un singolo file annexed, con la mappa dei remote su cui e' (o si vorrebbe metterlo)
 */
class AnnexedFile { /*extends File*/
    private char[] remotes;
}
