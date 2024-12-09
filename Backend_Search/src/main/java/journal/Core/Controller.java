package journal.Core;

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
    @Path("patients-by-name")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<PatientData> getPatientsByName(@QueryParam("name") String name) {
        return hapiService.getPatientsByName(name)
                .onItem().transform(hapiService::getPatientData)
                .onFailure()
                .recoverWithItem(() -> new PatientData("Error occurred while fetching patients", "", null, "", "", "", "", ""));
    }

    @GET
    @Path("practitioner-patients-by-name")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPractitionerPatientsByName(@QueryParam("name") String name, @QueryParam("practitioner") String practitioner) {
        try {
            List<Patient> patients = hapiService.getPatientsByNameAndPractitionerIdentifier(name, practitioner);
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("practitioners-by-name")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPractitionersByName(@QueryParam("name") String name) {
        try {
            List<Practitioner> practitioners = hapiService.getPractitionersByName(name);
            List<PractitionerData> practitionersData = new ArrayList<>();
            for (Practitioner practitioner : practitioners) {
                practitionersData.add(hapiService.getPractitionerData(practitioner));
            }
            return Response.ok(practitionersData).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("patients-by-condition")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientsByCondition(@QueryParam("condition") String condition) {
        try {
            List<Patient> patients = hapiService.getPatientsByConditionCode(condition);
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("practitioner-patients-by-condition")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPractitionerPatientsByCondition(@QueryParam("condition") String condition, @QueryParam("practitioner") String practitioner) {
        try {
            List<Patient> patients = hapiService.getPatientsByConditionCodeAndPractitionerIdentifier(condition, practitioner);
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}