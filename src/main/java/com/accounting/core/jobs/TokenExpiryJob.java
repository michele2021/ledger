package com.accounting.core.jobs;

import com.accounting.commons.Message;
import com.accounting.commons.SystemConstants;
import com.accounting.commons.infra.jobs.CCJobParams;
import com.accounting.core.db.TokenExpiryDAO;
import com.accounting.core.entities.views.TokenExpiryView;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.queue.KafkaAccountingMessagesQueue;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TokenExpiryJob extends BaseJob {

    private final TokenExpiryDAO tokenExpiryDAO;
    private final KafkaAccountingMessagesQueue accountingQueue;
    // These are 109 test tokens (108 for CHARIS_UK_TEST and 1 for EPAY_IT_TEST) that have only 2 lines in the "Activated" journal.
    private final Set<String> EXCLUDED_TXREFERENCES_PROD = new HashSet<>(List.of("ctkn_8c6714a5-4e57-4dff-b0a9-62b770b3d4ec"
            ,"ctkn_28020d80-6221-4a09-be9b-0be94f5bd8f1"
            ,"ctkn_c904192f-a4ac-4700-8995-225ffcaf451d"
            ,"ctkn_2385cfc6-60f3-44e3-9837-a7804990bce1"
            ,"ctkn_b3231a22-df05-491f-aab0-4bc3dc52abe2"
            ,"ctkn_a054e55d-4584-4705-82f6-3892bd9c05e4"
            ,"ctkn_c0e6d15e-9b4a-48c5-948a-fada942c1041"
            ,"ctkn_af49e1c8-6a85-4baa-92e7-49f8bfde60ff"
            ,"ctkn_e63c60db-cfad-46fa-9f56-c19bd8d61aed"
            ,"ctkn_f1af5d1c-0c82-49aa-93fb-74700fa873bf"
            ,"ctkn_e77520ae-81bb-41f3-ba0f-fef0a85785e9"
            ,"ctkn_be529055-6ead-4ba0-95ff-e847ea549cc7"
            ,"ctkn_2d9dcc8b-3f79-4d7a-853f-68fbe8436496"
            ,"ctkn_15af1575-d6ac-4d9b-b19d-2a87ae6a9719"
            ,"ctkn_044e7285-03b0-4572-8352-c7660944078f"
            ,"ctkn_81a2371c-176e-4ea4-a63c-544c1c4b095b"
            ,"ctkn_689264b8-7c5c-4fa1-97fb-9b69dde284b7"
            ,"ctkn_9c3fe8ef-e807-4cc2-8038-d0b3c0b8cb2f"
            ,"ctkn_31386274-3826-427a-a438-1899cb8872f0"
            ,"ctkn_fffb38cd-cadf-416a-aa81-25fd018b2bc4"
            ,"ctkn_71fffbf9-2788-469c-939e-49b7da334cbe"
            ,"ctkn_9a0bb34f-ffb9-48c2-b286-793284d07363"
            ,"ctkn_155e8aa7-53dd-425a-ba31-8ab88303e59c"
            ,"ctkn_c3bf723a-1d27-48d0-9015-73ffa5af05ab"
            ,"ctkn_af9aade3-ab7c-41a8-8fa8-39f66a4d1104"
            ,"ctkn_341feabe-afff-4837-97fa-d06dc0123e59"
            ,"ctkn_fe0bd703-7008-47b2-934d-fe15f74d4844"
            ,"ctkn_2bab67f9-128e-4830-b877-93a9c37d8516"
            ,"ctkn_0cd27839-d3cf-4b2c-b66e-144a4de1029d"
            ,"ctkn_1fe7b0b8-906d-42ea-85b5-dc1872f6d832"
            ,"ctkn_aeb07e3d-01e6-46e2-93c0-1dc5994550ac"
            ,"ctkn_bbac2548-7785-4d47-a95e-168b353df396"
            ,"ctkn_d78c852b-519d-462b-9c27-4c396c63f4a6"
            ,"ctkn_9d9201a8-73cf-4e48-a2f6-d1cb46539d08"
            ,"ctkn_3e00b4b9-c450-4712-abfa-04436093b226"
            ,"ctkn_e21b060c-cb6c-4ddc-846f-90b3b9e68763"
            ,"ctkn_0902d378-e76e-4d64-b5c6-5217314c9df7"
            ,"ctkn_64245601-8219-4de1-af95-c54a8f25952e"
            ,"ctkn_8f5325ba-7827-456f-8423-8d7a4736d861"
            ,"ctkn_c9bce31a-6ff0-4ee6-8d07-d68953406395"
            ,"ctkn_2166216f-10cf-4430-928c-861fc455a482"
            ,"ctkn_e99cb10d-b6f6-40cf-828e-2ef014cae516"
            ,"ctkn_8a6ad8a0-7b36-4ce6-a09a-1d8ad12e4db2"
            ,"ctkn_3ffba559-13e7-4d05-af25-7db1e8129a6b"
            ,"ctkn_d3ea4921-283a-4d07-a89b-c2e2796710bb"
            ,"ctkn_416859ef-ec86-4e53-a813-51213f016fb8"
            ,"ctkn_d8a3fb01-05ad-4066-8bbb-8bb7e537344b"
            ,"ctkn_2ef82c21-7e86-4ef7-89f9-6ca07c4b94ff"
            ,"ctkn_f005f8f5-d0bf-4b0e-aa5b-fce72a36dae5"
            ,"ctkn_c41acb0f-5121-48ad-8fb8-7bd004e3d2a6"
            ,"ctkn_cd2e54fc-b6a9-4077-afb1-180f955d6233"
            ,"ctkn_bdad71c3-d8af-4b86-b252-225a72ec560a"
            ,"ctkn_5985ce6e-5947-4f8c-b3d4-7201381074ca"
            ,"ctkn_5be399da-dd23-46e9-b26b-2f893af992dc"
            ,"ctkn_d2803107-bc8f-490b-8342-4f46e2fe2f60"
            ,"ctkn_947035f5-0122-4e59-a3a2-a4f3d25488a5"
            ,"ctkn_dca00eed-93c7-46e4-b3f3-56bdb3f2878e"
            ,"ctkn_989c7532-8d98-4dff-ac51-c2808dde67a3"
            ,"ctkn_1a46506b-222d-4924-9169-8bc069d9062b"
            ,"ctkn_2d308aff-db4f-4a3d-8142-25f9225fd1e8"
            ,"ctkn_f3d8880e-606a-478e-95f6-e383ff73bb4f"
            ,"ctkn_8a2bc913-a6c4-42ac-be16-b37a649bdcbd"
            ,"ctkn_e51cbcb4-85b6-447b-93f2-5ac18a955b26"
            ,"ctkn_3d6fe48a-9d6d-4fb9-a2c8-2e55de96ef6a"
            ,"ctkn_ceb1482b-3c78-4edf-bbe1-6018762afad7"
            ,"ctkn_df37308b-be85-426f-a2b4-11cbd11b64a9"
            ,"ctkn_7fa0b27a-79a1-4f02-bb84-7a05022686ef"
            ,"ctkn_6a4f33e7-e67e-4b28-a8d3-4aeabdb52aa3"
            ,"ctkn_adbf0933-14f7-4e49-9017-7021ffb81ab4"
            ,"ctkn_6f7819f6-7e74-45a7-b4c6-589cb68f826f"
            ,"ctkn_54a79c67-ea76-4728-b1ff-6083ade7c625"
            ,"ctkn_4fef0713-0734-4dea-97ae-918a8f8795ec"
            ,"ctkn_8cd31983-408b-4cf1-8bc8-ce33fdb46225"
            ,"ctkn_a4c9801a-7465-4781-9a99-766d585a60d6"
            ,"ctkn_8db59af0-d68e-4210-8921-79bdffe7a138"
            ,"ctkn_a52cba1a-8c41-4cca-bc76-ab7115cac440"
            ,"ctkn_3ee044ce-d46f-472a-8f7a-ecfc12e1673e"
            ,"ctkn_25e11456-0c5a-47a1-a44a-28748881db60"
            ,"ctkn_8d28c74d-78a8-4b6f-8c58-6b67b2db5d74"
            ,"ctkn_327711f4-b697-4c5a-b65f-602d86205145"
            ,"ctkn_abab5aff-f421-439a-bebe-fd66d1ecbc36"
            ,"ctkn_742506db-73e1-4994-92b1-8371fab036f4"
            ,"ctkn_428ebc43-8cbb-4b01-bf57-347cd18be6c1"
            ,"ctkn_89304738-74bf-4794-b881-fa7454d0c6a7"
            ,"ctkn_c6ee02d5-1f77-49b7-9a77-122d86bae0d1"
            ,"ctkn_fd6465a3-c9e4-41dc-9ec7-f6c8c484006f"
            ,"ctkn_ffd4928a-b4e6-4af6-b3cb-47ffaa24227d"
            ,"ctkn_97c2e30e-3c7d-4a08-887f-0c67f25c39a1"
            ,"ctkn_24b233e8-0611-47c1-b8dd-7f49c4530ebb"
            ,"ctkn_19fbc979-c83e-4bd7-8e5a-4fe76201309b"
            ,"ctkn_e6764514-8c8a-4a80-9c91-9f853bb8efbb"
            ,"ctkn_5241e5c9-344e-4659-b8e1-0277a1581106"
            ,"ctkn_e81e899e-0ba7-460d-9949-497be13bf07a"
            ,"ctkn_7ad836c8-c95f-4e95-80c6-d94d9e59ac14"
            ,"ctkn_2264645e-ab86-4deb-98be-f23069981b5d"
            ,"ctkn_ce280360-5b7e-403f-a25e-2cbbd3493241"
            ,"ctkn_905a9a35-b35e-417d-ac9b-83deed57a28d"
            ,"ctkn_b94e3ad1-9454-4d6e-803c-8c5c1252c1d3"
            ,"ctkn_12e18d3e-ea82-4a98-ac43-b29c7d49e58c"
            ,"ctkn_8013ce1f-ef0a-41b7-9ca5-041015344c93"
            ,"ctkn_7a13c1dd-7f0b-4d64-855e-56c42020a68d"
            ,"ctkn_c8d46d11-bda6-4293-b0b2-55c9b5a3c4db"
            ,"ctkn_c5287648-200f-400d-8101-54950cb305db"
            ,"ctkn_5a55cac6-da96-4d83-9dcf-f9b62f5ab344"
            ,"ctkn_bf2db29c-fe32-4b13-8535-f7ded8ab8a8e"
            ,"ctkn_a6656e94-f4df-41bf-91dd-5817523c58c6"
            ,"ctkn_f7777c31-d512-47ee-beaa-c70ca2dec544"
            ,"ctkn_test_9cfdee7c-7ab6-4181-b9b4-4e3c3223474e"));

    @Inject
    public TokenExpiryJob(KafkaAccountingMessagesQueue accountingMessagesQueue, RepositoryHelper repositoryHelper) {
        this.name = SystemConstants.Jobs.TOKEN_EXPIRY;
        this.tokenExpiryDAO = repositoryHelper.getTokenExpiryDAO();
        this.jobsDAO = repositoryHelper.getJobsDAO();
        this.accountingQueue = accountingMessagesQueue;
    }

    @Override
    public void process(CCJobParams params) throws JobProcessingException {
        try {
            processInternal(params);
        } catch (Exception e) {
            throw new JobProcessingException("Exception while running the job: ", e);
        }
    }

    private void processInternal(CCJobParams params) {
        Boolean recovery = params.getBooleanValue(SystemConstants.JParam.recovery);
        Long txId = params.getLongValue(SystemConstants.JParam.txId);
        Boolean test = params.getBooleanValue(SystemConstants.JParam.test);
        List<TokenExpiryView> tokens = tokenExpiryDAO.getExpiredTokens(recovery, txId);
        // remove tokens in the "ignore" list
        tokens = tokens.stream()
                .filter(e -> !EXCLUDED_TXREFERENCES_PROD.contains(e.getTxReference()))
                .collect(Collectors.toList());

        info("Identified %s tokens to be expired: %s.", tokens.size(), tokens);
        if (test) {
            log.info("Running in test mode, nothing do to.");
            return;
        }
        for (TokenExpiryView token : tokens) {
            log.info("Sending message to process expiry for token with txId {}", token.getTxId());
            JsonObject message = buildMsg(Message.EventType.TOKEN_EXPIRY, token);
            var sendMessageResponse = accountingQueue.sendMessage(message.toString());
            log.info("Message submitted to the accounting processing queue with id: {}", sendMessageResponse);
        }
        info("Processing completed, %d messages have been submitted!", tokens.size());
    }

    private JsonObject buildMsg(Message.EventType type, TokenExpiryView token) {
        JsonObject message = new JsonObject();

        message.addProperty("type", type.toString());
        message.addProperty("accountCode", token.getAccountCode());
        message.addProperty("txReference", token.getTxReference());

        // add a timestamp to make the message unique (fifo queue have a deduplication feature)
        long millis = System.currentTimeMillis();
        message.addProperty("timestamp", millis);
        // message source
        message.addProperty("source", Message.Source.ACCOUNTING_CORE.toString());

        return message;
    }
}