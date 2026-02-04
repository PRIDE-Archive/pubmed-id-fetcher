package uk.ac.ebi.pride.pubmed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.pubmed.model.EupmcResponse;
import uk.ac.ebi.pride.pubmed.model.EupmcResultArray;
import uk.ac.ebi.pride.pubmed.model.Reference;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fetches the reference details of a PubMed ID from the EU PMC REST WS, and also constructs a reference line ('refline')
 * with summary information in a single String.
 */
@Slf4j
public class PubMedFetcher {

    /**
     * Gets the PubMed reference from EU PMC or Pubmed. The method first check EU PMC and if it doesn't find it, then
     * looks into Pubmed.
     *
     * @param pubmedId the PubMed ID to query for.
     * @return A {@link Reference}
     * @throws URISyntaxException problems querying the EU PMC WS
     * @throws IOException        any problems returning results from the EU PMC WS
     */
    public static Reference getPubMedSummary(String pubmedId) throws Exception {
        List<Reference> results = null;
        final String REQUEST_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=ext_id:" + pubmedId + "%20src:med&format=json";
        log.info("Requesting EUPMC WS using: " + REQUEST_URL);
        EupmcResponse response = performEupmcQuery(REQUEST_URL);
        if (response != null) {
            results = getReference(response);
        }
        if (response == null || results.isEmpty()) {
            ReferenceUtil.PubMedReference pubmedReference = ReferenceUtil.getPubmedReference(pubmedId);
            if (pubmedReference != null) {
                Reference reference = getReference(pubmedReference);
                log.debug("Refline result: " + reference.getReferenceLine());
                return reference;
            } else {
                throw new IOException("No proper response from EU PMC & ncbi for: " + REQUEST_URL);
            }
        }
        List<Reference> filteredList = results.stream().filter(r -> (r.getPmid() != null || r.getDoi() != null)).toList();
        if (filteredList.isEmpty()) {
            throw new IOException("No PMID OR DOI found in response from EU PMC for: " + REQUEST_URL);
        }
        Reference result = filteredList.getFirst();
        log.debug("Refline result: " + result.getReferenceLine());
        return result;
    }

    /**
     * Search Publication by Project accession. If more than two records found, get the oldest one
     *
     * @param projectAccession PRIDE PXD ID
     * @return references
     * @throws Exception
     */
    public static Reference getPubMedSummaryByProjectAccession(String projectAccession) throws Exception {
        List<Reference> results = null;
        final String REQUEST_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=" + projectAccession + "%20src:med&format=json&sort=FIRST_PDATE_D%20asc";
        log.info("Requesting EUPMC WS using: " + REQUEST_URL);
        EupmcResponse response = performEupmcQuery(REQUEST_URL);
        if (response != null) {
            results = getReference(response);
        }
        if (response == null || results.isEmpty()) {
            throw new IOException("No proper response from EU PMC for: " + REQUEST_URL);
        }
        List<Reference> filteredList = results.stream().filter(r -> (r.getPmid() != null || r.getDoi() != null)).toList();
        if (filteredList.isEmpty()) {
            throw new IOException("No PMID OR DOI found in response from EU PMC for: " + REQUEST_URL);
        }
        Reference result = filteredList.getFirst();
        log.debug("Refline result: " + result.getReferenceLine());
        return result;
    }

    /**
     * Gets the PubMed reference from Pubmed only
     *
     * @param pubmedId the PubMed ID to query for.
     * @return A {@link Reference} object with the reference information.
     * @throws URISyntaxException problems querying the EU PMC WS
     * @throws IOException        any problems returning results from the EU PMC WS
     */
    public static Reference getPubMedSummaryFromPubmed(String pubmedId) throws Exception {
        Reference result;
        ReferenceUtil.PubMedReference pubmedReference = ReferenceUtil.getPubmedReference(pubmedId);
        if (pubmedReference != null) {
            result = getReference(pubmedReference);
        } else {
            throw new IOException("No proper response from Pubmed for: " + pubmedId);
        }
        log.debug("Refline result: " + result.getReferenceLine());
        return result;
    }

    public static Reference getDoiSummary(String doi) throws URISyntaxException, IOException {
        List<Reference> results = null;
        final String REQUEST_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=doi:" + doi + "&format=json";
        log.info("Requesting EUPMC WS using: " + REQUEST_URL);
        EupmcResponse response = performEupmcQuery(REQUEST_URL);
        if (response != null) {
            results = getReference(response);
        }
        if (response == null || results.isEmpty()) {
            throw new IOException("No proper response from EU PMC for: " + REQUEST_URL);
        }
        List<Reference> filteredList = results.stream().filter(r -> (r.getPmid() != null || r.getDoi() != null)).toList();
        if (filteredList.isEmpty()) {
            throw new IOException("No PMID OR DOI found in response from EU PMC for: " + REQUEST_URL);
        }
        Reference result = filteredList.getFirst();
        log.debug("Refline result: " + result.getReferenceLine());
        return result;
    }

    /**
     * This method checks if the publication is pre-print or not.
     * If it is a pre-print, value in the PubType field in the response will be preprint
     *
     * @param doi DOI
     * @return boolean results
     */
    public static boolean isPreprintPublication(String doi) throws URISyntaxException, IOException {
        List<Reference> results = null;
        final String REQUEST_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=doi:" + doi + "&format=json";
        log.info("Requesting EUPMC WS using: " + REQUEST_URL);
        EupmcResponse response = performEupmcQuery(REQUEST_URL);

        if (response != null) {
            results = getReference(response);
        }
        if (response == null || results.isEmpty()) {
            throw new IOException("No proper response from EU PMC for: " + REQUEST_URL);
        }
        List<Reference> filteredList = results.stream().filter(Reference::isPreprint).toList();
        if (!filteredList.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Performs the query to the EU PMC WS.
     *
     * @param requestUrl the URL as a Struns, used for the  query.
     * @return the response from the EU PMC WS.
     * @throws URISyntaxException problems querying the EU PMC WS
     */
    private static EupmcResponse performEupmcQuery(String requestUrl) throws URISyntaxException {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        EupmcResponse result;
        try {
            URI uri = new URI(requestUrl);
            result = restTemplate.getForObject(uri, EupmcResponse.class);
        } catch (URISyntaxException e) {
            log.error("Problem performing EU PMC request: " + requestUrl);
            throw e;
        }
        return result;
    }


    public static ReferenceUtil.PubMedReference performPubmedQuery(String pubmedID) throws Exception {
        return ReferenceUtil.getPubmedReference(pubmedID);
    }

    /**
     * Extracts the EU PMC response into a reference, and constructs a reference line ('refline')
     *
     * @param response the EU PMC response.
     * @return the reference summary
     * @throws IOException exactly 1 hit is expected, otherwise this is a problem due to either no, or multiple, hits were returned.
     */
    private static List<Reference> getReference(EupmcResponse response) throws IOException {
        int hitCount = response.getHitCount();
        if (hitCount == 0) {
            return new ArrayList<>();
        }

        EupmcResultArray resultList = response.getResultList();
        return Arrays.stream(resultList.getResult()).map(eupmcResult -> Reference.builder()
                .pmid(eupmcResult.getPmid())
                .pmcid(eupmcResult.getPmcid())
                .title(eupmcResult.getTitle())
                .authorString(eupmcResult.getAuthorString())
                .volume(eupmcResult.getJournalVolume())
                .issue(eupmcResult.getIssue())
                .pubYear(eupmcResult.getPubYear())
                .journalTitle(eupmcResult.getJournalTitle())
                .doi(eupmcResult.getDoi())
                .pageInfo(eupmcResult.getPageInfo())
                .pubType(eupmcResult.getPubType())
                .build()).toList();

    }

    /**
     * This function transform a {@link ReferenceUtil.PubMedReference} search output into a
     * {@link Reference}.
     *
     * @param pubmedReference PubmedReference results
     * @return Reference
     */
    private static Reference getReference(ReferenceUtil.PubMedReference pubmedReference) {
        return Reference.builder()
                .pmid(pubmedReference.getPmid())
                .title(pubmedReference.getTitle())
                .authorString(pubmedReference.getAuthorString())
                .volume(pubmedReference.getVolume())
                .issue(pubmedReference.getIssue())
                .pubYear(pubmedReference.getDate())
                .journalTitle(pubmedReference.getSource())
                .doi(pubmedReference.getDoi())
                .pageInfo(pubmedReference.getPages())
                .build();
    }
}


