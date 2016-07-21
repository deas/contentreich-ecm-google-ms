package es.keensoft.alfresco.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.*;
import com.google.common.collect.ImmutableList;
import es.keensoft.alfresco.google.SafeSearchConfig.Likelihood;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleVisionWorker {
    private static final Logger logger = LoggerFactory.getLogger(GoogleVisionWorker.class);
    private String applicationName;
    private String credentialsJsonPath;
    private Integer maxResults;
    private String translateLanguage;
    private String translateApiKey;
    private SafeSearchConfig safeSearchConfig;

    public /*GoogleVisionBean*/ Map execute(byte[] data/*ContentReader reader*/) throws Exception {

        // if (isImage(reader)) {

        // File sourceFile = TempFileProvider.createTempFile(getClass().getSimpleName(), ".tmp");
        // reader.getContent(sourceFile);

        // GoogleVisionBean gvb = searchData(data);// Files.toByteArray(sourceFile));
        return translate(searchData(data));

    	/* } else {
            return null;
    	} */

    }

    private /*GoogleVisionBean*/ Map translate(/*GoogleVisionBean*/Map gvb) throws Exception {

        if (!translateLanguage.equals("")) {

            Translate translate = new Translate(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    null);

            if (gvb.get("labels") != null) {
                List<String> labels = new ArrayList<String>();
                for (String label : (List<String>) gvb.get("labels")) {
                    List<String> list = new ArrayList<String>();
                    list.add(label);
                    Translate.Translations.List requestTranslate = translate.translations().list(list, "es").setKey(translateApiKey);
                    String result = requestTranslate.execute().getTranslations().get(0).getTranslatedText();
                    if (result != null) {
                        labels.add(result);
                    }
                }
                gvb.put("labels", labels);//setLabels(labels);
            }

            if (gvb.get("text") != null) {
                List<String> text = new ArrayList<String>();
                for (String textItem : (List<String>) gvb.get("text")) {
                    List<String> list = new ArrayList<String>();
                    list.add(textItem);
                    Translate.Translations.List requestTranslate = translate.translations().list(list, "es").setKey(translateApiKey);
                    String result = requestTranslate.execute().getTranslations().get(0).getTranslatedText();
                    if (result != null) {
                        text.add(result);
                    }
                }
                gvb.put("text", text);
            }

            if (gvb.get("landmark") != null) {
                List<String> list = new ArrayList<String>();
                list.add((String) gvb.get("landmark"));
                Translate.Translations.List requestTranslate = translate.translations().list(list, "es").setKey(translateApiKey);
                String result = requestTranslate.execute().getTranslations().get(0).getTranslatedText();
                if (result != null) {
                    gvb.put("landmark", result);
                }
            }

        }

        return gvb;

    }

    public Vision getVisionService() throws Exception {
        try (InputStream is = credentialsJsonPath.startsWith("/")
                ? getClass().getResourceAsStream(credentialsJsonPath)
                : new URL(credentialsJsonPath).openStream()) {
            GoogleCredential credential =
                    GoogleCredential.fromStream(is).createScoped(VisionScopes.all());
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
                    .setApplicationName(applicationName)
                    .build();
        }
    }

    public Boolean isInappropriateContent(byte[] data) throws Exception {

        if (safeSearchConfig.getEnabled()) {

            Vision vision = getVisionService();
            AnnotateImageRequest request =
                    new AnnotateImageRequest()
                            .setImage(new Image().encodeContent(data))
                            .setFeatures(ImmutableList.of(
                                    new Feature()
                                            .setType("SAFE_SEARCH_DETECTION")));

            Vision.Images.Annotate annotate = vision.images().annotate(
                    new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
            annotate.setDisableGZipContent(true);

            BatchAnnotateImagesResponse response = annotate.execute();

            if (response.getResponses().get(0).getSafeSearchAnnotation() != null) {
                if (safeSearchConfig.getAdultLikelihoodLevel() != null) {
                    Likelihood l1 = Likelihood.valueOf(response.getResponses().get(0).getSafeSearchAnnotation().getAdult());
                    return Likelihood.isLikelyOrBetter(l1, safeSearchConfig.getAdultLikelihoodLevel());
                }
                if (safeSearchConfig.getMedicalLikelihoodLevel() != null) {
                    Likelihood l1 = Likelihood.valueOf(response.getResponses().get(0).getSafeSearchAnnotation().getMedical());
                    return Likelihood.isLikelyOrBetter(l1, safeSearchConfig.getMedicalLikelihoodLevel());
                }
                if (safeSearchConfig.getSpoofLikelihoodLevel() != null) {
                    Likelihood l1 = Likelihood.valueOf(response.getResponses().get(0).getSafeSearchAnnotation().getSpoof());
                    return Likelihood.isLikelyOrBetter(l1, safeSearchConfig.getSpoofLikelihoodLevel());
                }
                if (safeSearchConfig.getViolenceLikelihoodLevel() != null) {
                    Likelihood l1 = Likelihood.valueOf(response.getResponses().get(0).getSafeSearchAnnotation().getViolence());
                    return Likelihood.isLikelyOrBetter(l1, safeSearchConfig.getViolenceLikelihoodLevel());
                }
                return true;
            }
        }

        return false;

    }


    public /*GoogleVisionBean*/ Map<String, Object> searchData(byte[] data) throws Exception {

        Vision vision = getVisionService();

        /*GoogleVisionBean*/
        Map gvb = new HashMap();//GoogleVisionBean();

        AnnotateImageRequest request =
                new AnnotateImageRequest()
                        .setImage(new Image().encodeContent(data))
                        .setFeatures(ImmutableList.of(
                                new Feature()
                                        .setType("LABEL_DETECTION")
                                        .setMaxResults(maxResults),
                                new Feature()
                                        .setType("LOGO_DETECTION")
                                        .setMaxResults(1),
                                new Feature()
                                        .setType("TEXT_DETECTION")
                                        .setMaxResults(maxResults),
                                new Feature()
                                        .setType("LANDMARK_DETECTION")
                                        .setMaxResults(1)));
        Vision.Images.Annotate annotate = vision.images().annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
        annotate.setDisableGZipContent(true);

        BatchAnnotateImagesResponse response = annotate.execute();

        if (response.getResponses().get(0).getLabelAnnotations() != null) {
            List<String> labels = new ArrayList<String>();
            for (EntityAnnotation ea : response.getResponses().get(0).getLabelAnnotations()) {
                labels.add(ea.getDescription());
            }
            gvb.put("labels", labels);//setLabels(labels);
        }

        if (response.getResponses().get(0).getLandmarkAnnotations() != null) {
            for (EntityAnnotation ea : response.getResponses().get(0).getLandmarkAnnotations()) {
                gvb.put("landmark", ea.getDescription());//setLandmark(ea.getDescription());
            }
        }

        if (response.getResponses().get(0).getLogoAnnotations() != null) {
            for (EntityAnnotation ea : response.getResponses().get(0).getLogoAnnotations()) {
                gvb.put("logo", ea.getDescription());//setLogo(ea.getDescription());
            }
        }

        if (response.getResponses().get(0).getTextAnnotations() != null) {
            List<String> text = new ArrayList<String>();
            for (EntityAnnotation ea : response.getResponses().get(0).getTextAnnotations()) {
                text.add(ea.getDescription());
            }
            gvb.put("text", text);//setText(text);
        }
        logger.info("Got {} things", gvb.keySet().size());
        return gvb;

    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setCredentialsJsonPath(String credentialsJsonPath) {
        this.credentialsJsonPath = credentialsJsonPath;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public void setTranslateLanguage(String translateLanguage) {
        this.translateLanguage = translateLanguage;
    }

    public void setTranslateApiKey(String translateApiKey) {
        this.translateApiKey = translateApiKey;
    }

    public void setSafeSearchConfig(SafeSearchConfig safeSearchConfig) {
        this.safeSearchConfig = safeSearchConfig;
    }

    public SafeSearchConfig getSafeSearchConfig() {
        return safeSearchConfig;
    }

}