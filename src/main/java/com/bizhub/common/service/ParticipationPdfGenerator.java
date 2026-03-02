package com.bizhub.common.service;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.user.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Génère un PDF type "Fiche d'inscription" / "Attestation de participation"
 * avec logo, informations participant, formation et référence.
 */
public class ParticipationPdfGenerator {

    private static final Color BLUE = new Color(0x1E, 0x3A, 0x5F);
    private static final Color LIGHT_GRAY = new Color(0x66, 0x66, 0x66);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", Locale.FRANCE);

    /**
     * Génère un PDF attestation de participation (fiche d'inscription).
     * @return contenu du PDF en bytes, ou null en cas d'erreur
     */
    public byte[] generate(User user, Formation formation, Participation participation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Logo (optionnel) - placer dans src/main/resources/com/bizhub/images/logo.png
            Image logo = loadLogo();
            if (logo != null) {
                logo.scaleToFit(120, 50);
                logo.setAlignment(Element.ALIGN_CENTER);
                doc.add(logo);
                doc.add(Chunk.NEWLINE);
            } else {
                Paragraph brand = new Paragraph("BizHub", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLUE));
                brand.setAlignment(Element.ALIGN_CENTER);
                doc.add(brand);
                doc.add(Chunk.NEWLINE);
            }

            // Titre principal
            Paragraph title = new Paragraph("ATTESTATION DE PARTICIPATION",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BLUE));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            doc.add(title);

            doc.add(new Paragraph(" "));

            // "Est décerné(e) à"
            Paragraph label = new Paragraph("Est décerné(e) à / Is awarded to",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, LIGHT_GRAY));
            label.setAlignment(Element.ALIGN_CENTER);
            doc.add(label);

            String participantName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName().trim() : "Participant";
            Paragraph name = new Paragraph(participantName,
                    FontFactory.getFont(FontFactory.HELVETICA, 20, Font.ITALIC, BLUE));
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(4);
            name.setSpacingAfter(16);
            doc.add(name);

            // Détails formation
            String formationTitle = formation != null && formation.getTitle() != null ? formation.getTitle() : "Formation";
            String formationDesc = formation != null && formation.getDescription() != null ? formation.getDescription() : "";
            String text = "Pour avoir suivi avec assiduité la formation :\n\"" + formationTitle + "\"";
            if (!formationDesc.isBlank()) {
                text += "\n\n" + formationDesc;
            }
            Paragraph formationBlock = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK));
            formationBlock.setSpacingAfter(12);
            doc.add(formationBlock);

            // Tableau récapitulatif
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(90);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            table.getDefaultCell().setBorderWidth(0.5f);
            table.getDefaultCell().setPadding(8);
            table.getDefaultCell().setBackgroundColor(new Color(0xF5, 0xF5, 0xF5));

            addTableRow(table, "Participant", participantName);
            addTableRow(table, "Email", user != null && user.getEmail() != null ? user.getEmail() : "-");
            addTableRow(table, "Formation", formationTitle);
            if (formation != null && formation.getStartDate() != null) {
                addTableRow(table, "Date début", formation.getStartDate().format(DATE_FORMAT));
            }
            if (formation != null && formation.getEndDate() != null) {
                addTableRow(table, "Date fin", formation.getEndDate().format(DATE_FORMAT));
            }
            if (formation != null && formation.getLieu() != null && !formation.getLieu().isBlank()) {
                addTableRow(table, "Lieu", formation.getLieu());
            }
            if (participation != null && participation.getAmount() != null) {
                addTableRow(table, "Montant", participation.getAmount().toPlainString() + " TND");
            }
            String dateValidation = participation != null && participation.getPaidAt() != null
                    ? participation.getPaidAt().format(DATETIME_FORMAT)
                    : "-";
            addTableRow(table, "Date de validation", dateValidation);

            doc.add(table);

            // Référence et pied de page
            String ref = participation != null && participation.getId() > 0
                    ? ("Réf. participation N° " + participation.getId())
                    : ("Réf. " + (participation != null && participation.getPaymentRef() != null ? participation.getPaymentRef() : "-"));
            String certifiedDate = participation != null && participation.getPaidAt() != null
                    ? participation.getPaidAt().format(DATE_FORMAT)
                    : java.time.LocalDate.now().format(DATE_FORMAT);

            Paragraph footer = new Paragraph();
            footer.add(new Chunk(ref + "  |  Certifié le : " + certifiedDate + "  |  www.bizhub.tn",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, LIGHT_GRAY)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
        cellLabel.setBorderWidth(0.5f);
        cellLabel.setPadding(6);
        cellLabel.setBackgroundColor(new Color(0xE8, 0xE8, 0xE8));
        table.addCell(cellLabel);
        PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "-", FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        cellValue.setBorderWidth(0.5f);
        cellValue.setPadding(6);
        table.addCell(cellValue);
    }

    private Image loadLogo() {
        String[] paths = { "/com/bizhub/images/logo.png", "/images/logo.png", "/com/bizhub/fxml/../images/site-images/logo.png" };
        for (String path : paths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    return Image.getInstance(is.readAllBytes());
                }
            } catch (Exception ignored) {
                // try next path
            }
        }
        return null;
    }
}
