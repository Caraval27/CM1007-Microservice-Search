package journal.Core;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import journal.Core.Model.PatientData;
import journal.Core.Model.PractitionerData;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.ArrayList;
import java.util.List;

@Path("/")
public class Controller {

    @Inject
    HapiService hapiService;

    @GET
    @Blocking
    @Path("patients-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<List<PatientData>> getPatientsByName(@QueryParam("name") String name) {
        return hapiService.getPatientsByName(name)
                .onItem().transform(patients -> patients.stream().map(patient -> hapiService.getPatientData(patient)).toList())
                .onFailure().recoverWithMulti(failure -> Multi.createFrom().empty());
    }

    @GET
    @Blocking
    @Path("practitioner-patients-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<List<PatientData>> getPractitionerPatientsByName(@QueryParam("name") String name, @QueryParam("practitioner") String practitioner) {
        return hapiService.getPatientsByNameAndPractitionerIdentifier(name, practitioner)
                .onItem().transform(patients -> patients.stream().map(patient -> hapiService.getPatientData(patient)).toList())
                .onFailure().recoverWithMulti(failure -> Multi.createFrom().empty());
    }

    @GET
    @Blocking
    @Path("patients-by-condition")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<List<PatientData>> getPatientsByCondition(@QueryParam("condition") String condition) {
        return hapiService.getPatientsByConditionCode(condition)
                .onItem().transform(patients -> patients.stream().map(patient -> hapiService.getPatientData(patient)).toList())
                .onFailure().recoverWithMulti(failure -> Multi.createFrom().empty());
    }

    @GET
    @Blocking
    @Path("practitioner-patients-by-condition")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<List<PatientData>> getPractitionerPatientsByCondition(@QueryParam("condition") String condition, @QueryParam("practitioner") String practitioner) {
        return hapiService.getPatientsByConditionCodeAndPractitionerIdentifier(condition, practitioner)
                .onItem().transform(patients -> patients.stream().map(patient -> hapiService.getPatientData(patient)).toList())
                .onFailure().recoverWithMulti(failure -> Multi.createFrom().empty());
    }

    @GET
    @Blocking
    @Path("practitioners-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<List<PractitionerData>> getPractitionersByName(@QueryParam("name") String name) {
        return hapiService.getPractitionersByName(name)
                .onItem().transform(practitioners -> practitioners.stream().map(practitioner -> hapiService.getPractitionerData(practitioner)).toList())
                .onFailure().recoverWithMulti(failure -> Multi.createFrom().empty());
    }
}