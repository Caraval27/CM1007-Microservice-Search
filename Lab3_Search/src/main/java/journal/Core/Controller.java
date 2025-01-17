package journal.Core;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import journal.Core.Model.PatientData;
import journal.Core.Model.PractitionerData;

@Authenticated
@Path("/")
public class Controller {

    @Inject
    HealthService healthService;

    @GET
    @Blocking
    @Path("patients-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PatientData> getPatientsByName(@QueryParam("name") String name) {
        return healthService.getPatientsByName(name)
                .onItem().transform(patient -> healthService.getPatientData(patient));
    }

    @GET
    @Blocking
    @Path("practitioner-patients-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PatientData> getPractitionerPatientsByName(@QueryParam("name") String name, @QueryParam("practitioner") String practitioner) {
        return healthService.getPatientsByNameAndPractitionerIdentifier(name, practitioner)
                .onItem().transform(patient -> healthService.getPatientData(patient));
    }

    @GET
    @Blocking
    @Path("patients-by-condition")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PatientData> getPatientsByCondition(@QueryParam("condition") String condition) {
        return healthService.getPatientsByConditionCode(condition)
                .onItem().transform(patient -> healthService.getPatientData(patient));
    }

    @GET
    @Blocking
    @Path("practitioner-patients-by-condition")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PatientData> getPractitionerPatientsByCondition(@QueryParam("condition") String condition, @QueryParam("practitioner") String practitioner) {
        return healthService.getPatientsByConditionCodeAndPractitionerIdentifier(condition, practitioner)
                .onItem().transform(patient -> healthService.getPatientData(patient));
    }

    @GET
    @Blocking
    @Path("practitioners-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PractitionerData> getPractitionersByName(@QueryParam("name") String name) {
        return healthService.getPractitionersByName(name)
                .onItem().transform(practitioner -> healthService.getPractitionerData(practitioner));
    }
}