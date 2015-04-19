import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.*;

public class GitAnnexGUI extends JFrame {
    private File originDir; // da "prependere" quando si esegue comando git-annex

    private ArrayList<AnnexedFile> annexedFiles;
    private ArrayList<String> remotes;

    public GitAnnexGUI(File f) {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);

        // TODO: usare JList!!!
        //add(new JList(...));

        annexedFiles=new ArrayList<AnnexedFile>();
        remotes=new ArrayList<String>();

        initFromAnnex(f);
    }

    public void initFromAnnex(File f) {
        try {
            //TODO: check if it is a git-annex!

            Process process=Runtime.getRuntime().exec("git-annex list",null,f);
            //InputStream stderr=process.getErrorStream();
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));

            //while(stderr.available()>0)System.err.println(stderr.readLine());
            String item;
            while((item=stdout.readLine())!=null) {
                System.out.println(item);
                annexedFiles.add(new AnnexedFile(item));
            }
        } catch(Exception e) {
            e.printStackTrace();
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
class AnnexedFile {
    private String nome;
    private char[] remotes; // TODO: a parte 'X' decidere una semantica

    /** si inizializza direttamente dalla stringa di git annex list
     */
    public AnnexedFile(String annexItem) {
        System.err.println(annexItem);
        String[] st=annexItem.split(" ");
        remotes=st[0].toCharArray();
        nome=st[1];
    }
}
