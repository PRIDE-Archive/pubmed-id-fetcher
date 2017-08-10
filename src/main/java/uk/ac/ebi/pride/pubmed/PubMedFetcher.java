package uk.ac.ebi.pride.pubmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.pubmed.model.EupmcResponse;
import uk.ac.ebi.pride.pubmed.model.EupmcResult;
import uk.ac.ebi.pride.pubmed.model.EupmcReferenceSummary;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class PubMedFetcher {
  private static Logger log = LoggerFactory.getLogger(PubMedFetcher.class);

  public static EupmcReferenceSummary getPubMedSummary(String pubmedId) throws URISyntaxException, IOException {
    EupmcReferenceSummary result;
    final String REQUEST_URL = "http://www.ebi.ac.uk/europepmc/webservices/rest/search?query=ext_id:" + pubmedId + "%20src:med&format=json";
    log.info("Requesting EUPMC WS using: " + REQUEST_URL);
    EupmcResponse response = performEupmcQuery(REQUEST_URL);
    if (response!=null) {
      result = maptoSummaryObject(response);
    } else {
      throw new IOException("No proper response from EU PMC for: " + REQUEST_URL);
    }
    log.debug("Refline result: " + result.getRefLine());
    return result;
  }

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

  private static EupmcReferenceSummary maptoSummaryObject(EupmcResponse request) throws IOException {
    EupmcReferenceSummary summaryResult = new EupmcReferenceSummary();
    int hitCount = request.getHitCount();
    if (hitCount!=1) {
      String message = "Error, expected 1 result back from EU PMC instead got: " + hitCount;
      log.error("Error, expected 1 result back from EU PMC instead got: " + hitCount);
      throw new IOException(message);
    }
    EupmcResult eupmcResult = request.getResultList().getResult()[0];
    StringBuilder refLine = new StringBuilder();
    refLine.append(eupmcResult.getAuthorString());
    refLine.append(' ');
    refLine.append(eupmcResult.getTitle());
    refLine.append(' ');
    refLine.append(eupmcResult.getJournalTitle());
    refLine.append('.');
    refLine.append(' ');
    refLine.append(eupmcResult.getPubYear());
    if (!StringUtils.isEmpty(eupmcResult.getJournalVolume())) {
      refLine.append(' ');
      refLine.append(eupmcResult.getJournalVolume());
    }
    if (!StringUtils.isEmpty(eupmcResult.getIssue())) {
      refLine.append('(');
      refLine.append(eupmcResult.getIssue());
      refLine.append(')');
    }
    if (!StringUtils.isEmpty(eupmcResult.getPageInfo())) {
      refLine.append(':');
      refLine.append(eupmcResult.getPageInfo());
    }
    summaryResult.setRefLine(refLine.toString());
    summaryResult.setEupmcResult(eupmcResult);
    return summaryResult;
  }
}


