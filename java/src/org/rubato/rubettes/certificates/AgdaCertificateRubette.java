/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.rubettes.certificates;

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.rubato.base.FileProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.global.NerveRubette;

/**
 * Turns properties of a Score into Agda proof certificates (Flieder, arXiv
 * 2512.04090, Ex. 2.4, B.2 and B.3): all-interval, scale membership, and the
 * dominant triad containing the leading tone. The view shows the verdicts and
 * the module as text, and an optional file receives the module; the rubette
 * does not run Agda (spec D9). See {@link CertificateReports}; the departures
 * are in sources/AGDA-CERTIFICATES-SPEC.md.
 */
public class AgdaCertificateRubette extends SimpleAbstractRubette {

    static final String N_KEY = "modulus";
    static final String TONIC_KEY = "tonic";
    static final String MODE_KEY = "mode";
    static final String MODULE_KEY = "module";
    static final String FILE_KEY = "file";

    private static final ImageIcon icon = Icons.loadIcon(NerveRubette.class, "nerveicon.png");

    private static final Border UNFOCUSED = new EmptyBorder(4, 6, 4, 6);

    /** The report's border while it has the keyboard focus. */
    static Border focusedBorder(JTextArea t) {
        return new CompoundBorder(new LineBorder(t.getForeground(), 2), new EmptyBorder(2, 4, 2, 4));
    }

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public AgdaCertificateRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new IntegerProperty(N_KEY, "n of Z_n for the all-interval check (2 to 24)", 12, 2, 24));
        putProperty(new IntegerProperty(TONIC_KEY, "Tonic of the key, a pitch class (0 = C to 11 = B)", 0, 0, 11));
        putProperty(new StringProperty(MODE_KEY, "Mode: major, harmonic minor or natural minor", "major", 1, 20));
        putProperty(new StringProperty(MODULE_KEY, "Agda module name (a capital letter, then letters and digits)", "Certificate", 1, 40));
        putProperty(new FileProperty(FILE_KEY, "Write the module to (optional; the name must be <module>.agda)", new String[] { "agda" }, true));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Certifies the Score (null when the input is not linked) and writes the module if a file is set. */
    public void process(Denotator score) {
        try {
            CertificateReports.Settings s = settings();
            CertificateReports.Report r = CertificateReports.analyze(score, s);
            String written = "";
            File f = ((FileProperty) getProperty(FILE_KEY)).getFile();
            if (f != null) {
                String expected = s.moduleName + ".agda";
                if (!f.getName().equals(expected)) {
                    throw new IllegalArgumentException("Agda needs the file of module " + s.moduleName + " to be named "
                                                       + expected + ", not " + f.getName() + "; nothing was written.");
                }
                try {
                    Files.write(f.toPath(), r.module().getBytes(StandardCharsets.UTF_8));
                }
                catch (IOException e) {
                    throw new IllegalArgumentException("Could not write " + f.getPath() + ": " + e.getMessage());
                }
                written = "Written to " + f.getPath() + "\n\n";
            }
            reportText = written + r.text();
            info = r.summary();
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
        if (text != null) {
            text.setText(reportText);
            text.setCaretPosition(0);
        }
    }

    CertificateReports.Settings settings() {
        return new CertificateReports.Settings(((IntegerProperty) getProperty(N_KEY)).getInt(),
                                               ((IntegerProperty) getProperty(TONIC_KEY)).getInt(),
                                               ((StringProperty) getProperty(MODE_KEY)).getString(),
                                               ((StringProperty) getProperty(MODULE_KEY)).getString().trim());
    }

    /** Sets the properties directly, for tests and shot drivers; out may be null. */
    public void setSettingsForTest(int n, int tonic, String mode, String module, File out) {
        ((IntegerProperty) getProperty(N_KEY)).setInt(n);
        ((IntegerProperty) getProperty(TONIC_KEY)).setInt(tonic);
        ((StringProperty) getProperty(MODE_KEY)).setString(mode);
        ((StringProperty) getProperty(MODULE_KEY)).setString(module);
        ((FileProperty) getProperty(FILE_KEY)).setFile(out);
    }

    /** The text the view shows. */
    public String getReportText() {
        return reportText;
    }

    /** The report area, after {@link #getView}. */
    public JTextArea getTextArea() {
        return text;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            text = new JTextArea(22, 80);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(UNFOCUSED);
            // the caret alone is too faint a focus indicator; a 2 px line in the text colour
            // replaces 2 px of the padding, so the text does not move
            text.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    text.setBorder(focusedBorder(text));
                }

                public void focusLost(FocusEvent e) {
                    text.setBorder(UNFOCUSED);
                }
            });
            text.getAccessibleContext().setAccessibleName("Agda certificate report");
            text.getAccessibleContext().setAccessibleDescription(
                "The verdicts on the Score and the key, then the Agda module that proves them, as text.");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Agda certificates"));
            view.add(scroll, BorderLayout.CENTER);
        }
        text.setText(reportText);
        text.setCaretPosition(0);
        return view;
    }

    public void updateView() {
        getView();
    }

    public boolean hasInfo() {
        return true;
    }

    public String getInfo() {
        return info;
    }

    public String getGroup() {
        return "Score";
    }

    public String getName() {
        return "Agda Certificate";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Writes Agda proofs that a Score is all-interval, lies in a key, and of the key's dominant";
    }

    public String getLongDescription() {
        return "The Agda Certificate rubette reads the pitch classes of a Score and decides three"
             + " properties from Flieder's type-theoretic account (arXiv 2512.04090): whether the"
             + " pitch-class set mod n contains every interval class (all-interval, Ex. B.2), whether"
             + " every note lies in the key's scale (Ex. 2.4), and whether the key's dominant triad"
             + " contains its leading tone (Ex. B.3; vacuous in natural minor). Each verdict comes"
             + " with an Agda definition whose type is the claim and whose body is the proof. The"
             + " view shows the verdicts and the module; set a file to save the module, then check"
             + " it with spec/agda/check-certificate.sh. The rubette does not run Agda itself.";
    }

    public String getInTip(int i) {
        return "Score (optional)";
    }

    public String getOutTip(int i) {
        return "";
    }
}
