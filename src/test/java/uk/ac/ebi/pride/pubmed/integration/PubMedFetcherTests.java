package uk.ac.ebi.pride.pubmed.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;


import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import uk.ac.ebi.pride.pubmed.PubMedFetcher;
import uk.ac.ebi.pride.pubmed.model.Reference;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Tests obtaining references from the EU PMC WS.
 */
@Slf4j
public class PubMedFetcherTests {

  /**
   * Test to obtain references from the EU PMC WS.
   * @throws Exception any problems obtaining the reference details.
   */
  @Test
  public void obtainReferences() throws Exception {
    assertSummaryReference(PubMedFetcher.getPubMedSummary("22147733"));
    assertSummaryReference(PubMedFetcher.getPubMedSummary("22807455"));
    assertSummaryReference(PubMedFetcher.getPubMedSummary("22825847"));
    assertSummaryReference(PubMedFetcher.getPubMedSummary("24607996"));
    assertSummaryReference(PubMedFetcher.getPubMedSummary("26793209"));
  }

  /**
   * Checks the reference summary information are not null.
   * @param summary the summary reference to check
   */
  private void assertSummaryReference(Reference summary) throws Exception {
    Assert.isTrue(summary!=null, "Summary cannot be null!");
    Assert.isTrue(!StringUtils.isEmpty(summary.getReferenceLine()), "Summary RefLine cannot be null or empty!");
    Assert.isTrue((PubMedFetcher.getPubMedSummaryFromPubmed("22147733") != null), "");
    Assert.isTrue(summary !=null, "Summary Result cannot be null!");
    log.info("Summary reference: " + summary.getReferenceLine());
    log.info("Summary reference: " + PubMedFetcher.getPubMedSummaryFromPubmed("22147733").getReferenceLine());
  }

    /**
     * Check publications are pre-print or not
     * @throws IOException
     * @throws URISyntaxException
     */
  @Test
  public void isPreprintPublication() throws IOException, URISyntaxException {
    Assert.isTrue(PubMedFetcher.isPreprintPublication("10.1101/846303"),"Not a Preprint");
    Assert.isTrue(! PubMedFetcher.isPreprintPublication("10.1074/mcp.M111.013797"),"A Preprint");
  }

  @Test
  public void isPubMedSummaryByProjectAccession() throws Exception {
    Assert.isTrue(PubMedFetcher.getPubMedSummaryByProjectAccession("PXD025705").getPmid().equals("35508466"), "");
  }
}
