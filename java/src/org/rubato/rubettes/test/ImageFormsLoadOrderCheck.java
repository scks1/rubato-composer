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

package org.rubato.rubettes.test;

import java.io.File;

import org.rubato.base.FileProperty;
import org.rubato.base.Rubette;
import org.rubato.base.RubetteProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.rubette.Link;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.builtin.RealArithRubette;
import org.rubato.rubettes.image.ImageFileInRubette;
import org.rubato.rubettes.image.ImageFileOutRubette;
import org.rubato.rubettes.util.CoolFormRegistrant;

/**
 * Registers BigBang's forms and runs ImageFileIn into ImageFileOut. CustomPropertiesFollowupsTest
 * loads this class in a class loader of its own, after the image rubette classes, so the forms
 * are registered only after those classes have loaded.
 */
public class ImageFormsLoadOrderCheck {

    private static final RunInfo RUN_INFO = new RunInfo() {
        public boolean stopped() { return false; }
        public void addMessage(RubetteModel rubette, String msg) { }
    };

    private static void setFile(SimpleAbstractRubette r, File f) {
        for (RubetteProperty p : r.getRubetteProperties().getProperties()) {
            if (p instanceof FileProperty) {
                ((FileProperty) p).setFile(f);
            }
        }
        r.applyProperties();
    }

    private static void run(Rubette r, Denotator... inputs) {
        RubetteModel model = new RubetteModel(r, r.getName());
        for (int i = 0; i < inputs.length; i++) {
            RealArithRubette source = new RealArithRubette();
            source.setOutput(0, inputs[i]);
            RubetteModel sourceModel = new RubetteModel(source, "source" + i);
            model.setInLink(new Link(sourceModel, 0, model, i));
        }
        r.clearErrors();
        r.run(RUN_INFO);
    }

    /**
     * Returns "OK" when ImageFileIn reads the png and ImageFileOut writes its pixels to out, and
     * otherwise a description of the first problem.
     */
    public static String registerThenRun(String png, String out) {
        new CoolFormRegistrant().registerAllTheCoolStuff();
        ImageFileInRubette in = new ImageFileInRubette();
        setFile(in, new File(png));
        run(in);
        if (in.hasErrors()) {
            return "ImageFileIn: " + in.getErrors();
        }
        if (in.getOutput(0) == null) {
            return "ImageFileIn: no output";
        }
        ImageFileOutRubette o = new ImageFileOutRubette();
        setFile(o, new File(out));
        run(o, in.getOutput(0));
        if (o.hasErrors()) {
            return "ImageFileOut: " + o.getErrors();
        }
        if (!new File(out).exists()) {
            return "ImageFileOut: wrote no file";
        }
        return "OK";
    }
}
