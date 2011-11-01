/**
 * Copyright (C) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.quickstart.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JDialog;

import org.onebusaway.quickstart.bootstrap.gui.BootstrapDataModel;
import org.onebusaway.quickstart.bootstrap.gui.GtfsPathWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.GtfsRealtimePathsWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.QuickStartTypeWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.RunWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.TransitDataBundlePathWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.WelcomeWizardPanelController;
import org.onebusaway.quickstart.bootstrap.gui.wizard.WizardController;
import org.onebusaway.quickstart.bootstrap.gui.wizard.WizardController.ECompletionState;
import org.onebusaway.quickstart.bootstrap.gui.wizard.WizardDialog;

/**
 * 
 * @author bdferris
 */
public class GuiBootstrapMain {
  public static void main(String[] args) throws IOException, Exception {

    final BootstrapDataModel model = new BootstrapDataModel();
    loadModel(model);

    WizardController controller = new WizardController();
    controller.addPanel(WelcomeWizardPanelController.class,
        new WelcomeWizardPanelController());
    controller.addPanel(TransitDataBundlePathWizardPanelController.class,
        new TransitDataBundlePathWizardPanelController(model, controller));
    controller.addPanel(QuickStartTypeWizardPanelController.class,
        new QuickStartTypeWizardPanelController(model));
    controller.addPanel(GtfsPathWizardPanelController.class,
        new GtfsPathWizardPanelController(model, controller));
    controller.addPanel(GtfsRealtimePathsWizardPanelController.class,
        new GtfsRealtimePathsWizardPanelController(model));
    controller.addPanel(RunWizardPanelController.class,
        new RunWizardPanelController(model));
    controller.setCurrentPanel(WelcomeWizardPanelController.class);

    WizardDialog dialog = new WizardDialog(controller);
    dialog.setModal(true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);

    saveModel(model);

    if (controller.getCompletionState() == ECompletionState.CANCELLED)
      System.exit(0);

    if (model.isBuildEnabled()) {
      System.out.println("=== BUILDING A TRANSIT DATA BUNDLE ===");
      String gtfsPath = model.getGtfsPath();
      String bundlePath = model.getTransitDataBundlePath();
      BuildBootstrapMain.main(new String[] {gtfsPath, bundlePath});
    }
    
    if (model.isRunEnabled()) {
      System.out.println("=== RUNNING THE WEBAPP ===");
      ProtectionDomain protectionDomain = GuiBootstrapMain.class.getProtectionDomain();
      URL warUrl = protectionDomain.getCodeSource().getLocation();
      List<String> runArgs = new ArrayList<String>();
      if (model.getTripUpdatesUrl() != null) {
        runArgs.add("-gtfsRealtimeTripUpdatesUrl=" + model.getTripUpdatesUrl());
      }
      if (model.getVehiclePositionsUrl() != null) {
        runArgs.add("-gtfsRealtimeVehiclePositionsUrl="
            + model.getVehiclePositionsUrl());
      }
      if (model.getAlertsUrl() != null) {
        runArgs.add("-gtfsRealtimeAlertsUrl=" + model.getAlertsUrl());
      }
      runArgs.add(model.getTransitDataBundlePath());
      WebappBootstrapMain.run(warUrl,
          runArgs.toArray(new String[runArgs.size()]));
    }
  }

  private static void loadModel(BootstrapDataModel model) {
    Preferences preferences = Preferences.userNodeForPackage(GuiBootstrapMain.class);
    model.setTransitDataBundlePath(preferences.get("transitDataBundlePath",
        null));
    model.setGtfsPath(preferences.get("gtfsPath", null));
    model.setTripUpdatesUrl(preferences.get("tripUpdatesUrl", null));
    model.setVehiclePositionsUrl(preferences.get("vehiclePositionsUrl", null));
    model.setAlertsUrl(preferences.get("alertsUrl", null));
  }

  private static void saveModel(BootstrapDataModel model) {
    Preferences preferences = Preferences.userNodeForPackage(GuiBootstrapMain.class);
    updatePreferences(preferences, "transitDataBundlePath",
        model.getTransitDataBundlePath());
    updatePreferences(preferences, "gtfsPath", model.getGtfsPath());
    updatePreferences(preferences, "tripUpdatesUrl", model.getTripUpdatesUrl());
    updatePreferences(preferences, "vehiclePositionsUrl",
        model.getVehiclePositionsUrl());
    updatePreferences(preferences, "alertsUrl", model.getAlertsUrl());
    try {
      preferences.sync();
    } catch (BackingStoreException e) {
      e.printStackTrace();
    }
  }

  private static void updatePreferences(Preferences preferences,
      String property, String value) {
    if (value == null)
      preferences.remove(property);
    else
      preferences.put(property, value);
  }
}
