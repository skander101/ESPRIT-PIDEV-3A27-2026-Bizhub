module com.example.gestion_investissement {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires java.sql;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    // jbcrypt warning: Name of automatic module is unstable - acceptable for this project
    requires jbcrypt;
    // IMPORTANT: Ouvrir les packages aux modules JavaFX
    opens com.bizhub.Investistment.Controllers to javafx.fxml;
    opens com.bizhub.Investistment.Entitites to javafx.base;
    opens com.bizhub.formation.controller to javafx.fxml;
    opens com.bizhub.review.controller to javafx.fxml;
    opens com.bizhub.user.controller to javafx.fxml;

    // Exporter les packages
    exports com.bizhub.Investistment;
    exports com.bizhub.Investistment.Controllers;
    exports com.bizhub.Investistment.Entitites;
    exports com.bizhub.Investistment.Services.AI;
    exports com.bizhub.Investistment.Services;
    exports com.bizhub.formation.controller;
    exports com.bizhub.review.controller;
    exports com.bizhub.user.controller;
    exports com.bizhub.formation.model;
    exports com.bizhub.review.model;

    opens com.bizhub.Investistment to javafx.fxml;


}