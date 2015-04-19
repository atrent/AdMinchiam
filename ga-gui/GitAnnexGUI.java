import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.*;

public class GitAnnexGUI extends JFrame {
    private File originDir; // da "prependere" quando si esegue comando git-annex

    private Vector<AnnexedFile> annexedFiles;
    private Vector<Remote> remotes;

    public GitAnnexGUI(File f) {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);

        annexedFiles=new Vector<AnnexedFile>();
        remotes=new Vector<Remote>();

        initFromAnnex(f);

        // TODO: usare JList!!!
        JList r=new JList(remotes);
        r.setFont(Font.getFont(Font.MONOSPACED));
        add(r,BorderLayout.NORTH);

        JList fl=new JList(annexedFiles);
        fl.setFont(Font.getFont(Font.MONOSPACED));
        add(new JScrollPane(fl));
    }

    public void initFromAnnex(File f) {
        try {
            //TODO: check if it is a git-annex!

            Process process=Runtime.getRuntime().exec("git-annex list",null,f);
            //InputStream stderr=process.getErrorStream();
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));

            //while(stderr.available()>0)System.err.println(stderr.readLine());

            String item=stdout.readLine(); // il primo e' "here" (dovrebbe)
            if(item==null) return; // errore, non inizializzato
            remotes.add(new Remote(item));

            while((item=stdout.readLine())!=null) {
                //System.out.println(item);
                if(item.startsWith("|") && !item.endsWith("|")) {
                    //other remotes
                    remotes.add(new Remote(item));
                } else {
                    if(!item.endsWith("|")) {
                        // annexed items
                        annexedFiles.add(new AnnexedFile(item));
                    }
                }
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
    private String name;
    private char[] remotes; // TODO: a parte 'X' decidere una semantica

    /** si inizializza direttamente dalla stringa di git annex list
     */
    public AnnexedFile(String annexItem) {
        //System.err.println(annexItem);
        String[] st=annexItem.split(" ");
        remotes=st[0].toCharArray();
        name=st[1];
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append(remotes);
        sb.append(":");
        sb.append(name);
        return sb.toString();
    }
}


class Remote {
    private String name;

    /** pos e' ridondante rispetto alla posizione nel vector (in GUI), attenzione!
     */
    private int pos=0; // 0=here

    /** si inizializza direttamente dalla stringa di git annex list
     */
    public Remote(String annexItem) {
        System.err.println(annexItem);

        // controllo ridondante, ma non fa niente
        if(annexItem.startsWith("|") && !annexItem.endsWith("|")) {
            for(; annexItem.charAt(pos)=='|'; pos++);
            name=annexItem.substring(pos);
        } else {
            if(!annexItem.endsWith("|")) {
                pos=0; //here
                name=annexItem;
            }
        }
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append(new String(new char[pos+1]).replace("\0", "_")); // PADDING!!!!!!
        sb.append(name);
        sb.append(":");
        sb.append(pos);
        return sb.toString();
    }
}
