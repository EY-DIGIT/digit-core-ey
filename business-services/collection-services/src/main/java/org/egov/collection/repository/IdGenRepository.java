package org.egov.collection.repository;

import static org.egov.collection.config.CollectionServiceConstants.COLL_TRANSACTION_FORMAT;
import static org.egov.collection.config.CollectionServiceConstants.COLL_TRANSACTION_ID_NAME;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.IdGenerationRequest;
import org.egov.collection.model.IdGenerationResponse;
import org.egov.collection.model.IdRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.ServiceCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IdGenRepository {
	
    @Autowired
    private RestTemplate restTemplate;
    
	@Autowired
	private ApplicationProperties applicationProperties;

    /**
     * Generates a receipt number
     *  - If isReceiptNumberByService flag is set to true,
     *      generates receipt number by business service and tenant id
     *  - Else generates by tenant id only
     *
     * @param requestInfo
     * @param businessService
     * @param tenantId
     * @return
     */
	public String generateReceiptNumber(RequestInfo requestInfo, String businessService ,String tenantId) {
        String idName = "";
        String format = null;
	    log.debug("Attempting to generate Receipt Number from ID Gen");

        if(applicationProperties.isReceiptNumberByService()){
            idName = idName + businessService.toLowerCase() + "." + applicationProperties.getReceiptNumberIdNameImc();
        } else{
            idName = applicationProperties.getReceiptNumberIdNameImc();
            format = applicationProperties.getReceiptNumberStateLevelFormatImc();
        }
        String rcieptNumberSequence = getId(requestInfo, tenantId, idName, format, 1);
        String recieptId = generateImcRecieptNumber(rcieptNumberSequence);
        return recieptId;
//        return getId(requestInfo, tenantId, idName, format, 1);
	}

    public String generateTransactionNumber(RequestInfo requestInfo, String tenantId) {
        log.debug("Attempting to generate Transaction Number from ID Gen");

        String[] tenantArray = tenantId.split("\\.");
        String splitTenant = tenantId.contains(".") ? tenantArray[tenantArray.length-1] : tenantId;
        String tenantFormat = COLL_TRANSACTION_FORMAT.replace("{tenant}", splitTenant);

        return getId(requestInfo, tenantId, COLL_TRANSACTION_ID_NAME, tenantFormat, 1);

    }

    private String getId(RequestInfo requestInfo, String tenantId, String name, String format, int count) {

        List<IdRequest> reqList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            reqList.add(new IdRequest(name, tenantId, format));
        }
        IdGenerationRequest req = new IdGenerationRequest(requestInfo, reqList);
        String uri = UriComponentsBuilder
                .fromHttpUrl(applicationProperties.getIdGenServiceHost())
                .path(applicationProperties.getIdGeneration())
                .build()
                .toUriString();
        try {
            IdGenerationResponse idGenerationResponse = restTemplate.postForObject(uri, req,
                    IdGenerationResponse.class);
            return idGenerationResponse.getIdResponses().get(0).getId();
        } catch (HttpClientErrorException e) {
            log.error("ID Gen Service failure ", e);
            throw new ServiceCallException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("ID Gen Service failure", e);
            throw new org.egov.tracer.model.CustomException("IDGEN_SERVICE_ERROR", "Failed to generate ID, unknown error occurred");
        }
    }
    
    private String generateImcRecieptNumber( String sequence) {

		// YY - last 2 digits of current year
		String year = String.valueOf(Year.now().getValue()).substring(2);

		// MM - 2-digit month
		String month = String.format("%02d", LocalDate.now().getMonthValue());

		// DD - 2-digit day
		String day = String.format("%02d", LocalDate.now().getDayOfMonth());

        // Replace placeholders in sequence
        String recieptNumber = sequence
                .replace("[YY]", year)
                .replace("[MM]", month)
                .replace("[DD]", day);

        return recieptNumber;
    }
}
