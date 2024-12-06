package journal.Core;

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
    @Path("patients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatients() {
        try {
            List<Patient> patients = hapiService.getPatientsByIdentifierSystem();
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("practitioner_patients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPractitionerPatients(@QueryParam("id") String id) {
        try {
            List<Patient> patients = hapiService.getPractitionerPatientsByIdentifier(id);
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("practitioners")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPractitioners(@QueryParam("query") String query) {
        try {
            List<Practitioner> practitioners = hapiService.getPractitionersByIdentifierSystemAndQuery(query);
            List<PractitionerData> practitionersData = new ArrayList<>();
            for (Practitioner practitioner : practitioners) {
                practitionersData.add(hapiService.getPractitionerData(practitioner));
            }
            return Response.ok(practitionersData).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}