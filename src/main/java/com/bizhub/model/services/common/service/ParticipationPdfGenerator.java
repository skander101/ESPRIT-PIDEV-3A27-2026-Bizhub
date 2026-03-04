package com.bizhub.model.services.common.service;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.users_avis.user.User;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Génère un PDF type "Fiche d'inscription" / "Attestation de participation"
 * avec logo, informations participant, formation et référence.
 *
 * Uses iText 7 (already in f2's pom.xml) instead of OpenPDF.
 */
public class ParticipationPdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", Locale.FRANCE);

    /**
     * Génère un PDF attestation de participation (fiche d'inscription).
     * @return contenu du PDF en bytes, ou null en cas d'erreur
     */
    public byte[] generate(User user, Formation formation, Participation participation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc, com.itextpdf.kernel.geom.PageSize.A4);
            doc.setMargins(50, 40, 50, 40);

            com.itextpdf.kernel.colors.DeviceRgb blue = new com.itextpdf.kernel.colors.DeviceRgb(0x1E, 0x3A, 0x5F);
            com.itextpdf.kernel.colors.DeviceRgb lightGray = new com.itextpdf.kernel.colors.DeviceRgb(0x66, 0x66, 0x66);
            com.itextpdf.kernel.colors.DeviceRgb bgGray = new com.itextpdf.kernel.colors.DeviceRgb(0xE8, 0xE8, 0xE8);

            // Brand
            com.itextpdf.layout.element.Paragraph brand = new com.itextpdf.layout.element.Paragraph("BizHub")
                    .setFontSize(18).setBold().setFontColor(blue)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            doc.add(brand);
            doc.add(new com.itextpdf.layout.element.Paragraph("\n"));

            // Title
            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("ATTESTATION DE PARTICIPATION")
                    .setFontSize(22).setBold().setFontColor(blue)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginBottom(8);
            doc.add(title);

            // "Est décerné(e) à"
            doc.add(new com.itextpdf.layout.element.Paragraph("Est décerné(e) à / Is awarded to")
                    .setFontSize(10).setFontColor(lightGray)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            String participantName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName().trim() : "Participant";
            doc.add(new com.itextpdf.layout.element.Paragraph(participantName)
                    .setFontSize(20).setItalic().setFontColor(blue)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginTop(4).setMarginBottom(16));

            // Formation details
            String formationTitle = formation != null && formation.getTitle() != null ? formation.getTitle() : "Formation";
            String formationDesc = formation != null && formation.getDescription() != null ? formation.getDescription() : "";
            String text = "Pour avoir suivi avec assiduité la formation :\n\"" + formationTitle + "\"";
            if (!formationDesc.isBlank()) {
                text += "\n\n" + formationDesc;
            }
            doc.add(new com.itextpdf.layout.element.Paragraph(text).setFontSize(11).setMarginBottom(12));

            // Summary table
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(2).useAllAvailableWidth();
            table.setMarginTop(10).setMarginBottom(10);

            addTableRow(table, "Participant", participantName, bgGray);
            addTableRow(table, "Email", user != null && user.getEmail() != null ? user.getEmail() : "-", bgGray);
            addTableRow(table, "Formation", formationTitle, bgGray);
            if (formation != null && formation.getStartDate() != null) {
                addTableRow(table, "Date début", formation.getStartDate().format(DATE_FORMAT), bgGray);
            }
            if (formation != null && formation.getEndDate() != null) {
                addTableRow(table, "Date fin", formation.getEndDate().format(DATE_FORMAT), bgGray);
            }
            if (formation != null && formation.getLieu() != null && !formation.getLieu().isBlank()) {
                addTableRow(table, "Lieu", formation.getLieu(), bgGray);
            }
            if (participation != null && participation.getAmount() != null) {
                addTableRow(table, "Montant", participation.getAmount().toPlainString() + " TND", bgGray);
            }
            String dateValidation = participation != null && participation.getPaidAt() != null
                    ? participation.getPaidAt().format(DATETIME_FORMAT)
                    : "-";
            addTableRow(table, "Date de validation", dateValidation, bgGray);
            doc.add(table);

            // Footer
            String ref = participation != null && participation.getId() > 0
                    ? ("Réf. participation N° " + participation.getId())
                    : ("Réf. " + (participation != null && participation.getPaymentRef() != null ? participation.getPaymentRef() : "-"));
            String certifiedDate = participation != null && participation.getPaidAt() != null
                    ? participation.getPaidAt().format(DATE_FORMAT)
                    : java.time.LocalDate.now().format(DATE_FORMAT);

            doc.add(new com.itextpdf.layout.element.Paragraph(ref + "  |  Certifié le : " + certifiedDate + "  |  www.bizhub.tn")
                    .setFontSize(9).setFontColor(lightGray)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginTop(20));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void addTableRow(com.itextpdf.layout.element.Table table, String label, String value, com.itextpdf.kernel.colors.DeviceRgb bgColor) {
        com.itextpdf.layout.element.Cell cellLabel = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(label).setBold().setFontSize(10))
                .setBackgroundColor(bgColor).setPadding(6);
        table.addCell(cellLabel);

        com.itextpdf.layout.element.Cell cellValue = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(value != null ? value : "-").setFontSize(10))
                .setPadding(6);
        table.addCell(cellValue);
    }
}

