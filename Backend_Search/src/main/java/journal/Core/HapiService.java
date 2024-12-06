package journal.Core;

import ca.uhn.fhir.context.FhirContext;
import jakarta.enterprise.context.ApplicationScoped;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import journal.Core.Model.PatientData;
import journal.Core.Model.PractitionerData;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HapiService {
    private FhirContext context;
    private IGenericClient client;
    private static final String patientSystem = "http://electronichealth.se/identifier/personnummer";
    private static final String practitionerSystem = "http://terminology.hl7.org/CodeSystem/v2-0203";
    private static final String practitionerRoleSystem = "http://terminology.hl7.org/CodeSystem/practitioner-role";
    private static final String hapiServerURL = "https://hapi-fhir.app.cloud.cbh.kth.se/fhir";

    public HapiService() {
        this.context = FhirContext.forR4();
        this.client = context.newRestfulGenericClient(hapiServerURL);
    }

    public List<Patient> getPatientsByIdentifierSystem() {
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.hasSystemWithAnyCode(patientSystem))
                .sort().ascending(Patient.NAME)
                .returnBundle(Bundle.class)
                .execute();

        List<Patient> patients = new ArrayList<>(bundle.getEntry().stream()
                .map(p -> (Patient) p.getResource())
                .toList());

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            patients.addAll(bundle.getEntry().stream()
                    .map(p -> (Patient) p.getResource())
                    .toList());
        }
        return patients;
    }

    public List<Patient> getPractitionerPatientsByIdentifier(String identifierValue) {
        Practitioner practitioner = getPractitionerByIdentifier(identifierValue);
        if (practitioner == null) {
            return null;
        }

        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.GENERAL_PRACTITIONER.hasId("Practitioner/" + practitioner.getIdElement().getIdPart()))
                .sort().ascending(Patient.NAME)
                .returnBundle(Bundle.class)
                .execute();

        List<Patient> patients = new ArrayList<>(bundle.getEntry().stream()
                .map(p -> (Patient) p.getResource())
                .toList());

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            patients.addAll(bundle.getEntry().stream()
                    .map(p -> (Patient) p.getResource())
                    .toList());
        }
        return patients;
    }

    public Practitioner getPractitionerByIdentifier(String identifierValue) {
        Bundle bundle = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().systemAndIdentifier(practitionerSystem, identifierValue))
                .returnBundle(Bundle.class)
                .execute();
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        if (entries.isEmpty()) {
            return null;
        }
        return (Practitioner) entries.get(0).getResource();
    }

    public PatientData getPatientData(Patient patient) {
        if (patient == null) {
            return null;
        }
        String ssn = "";
        if (patient.hasIdentifier()) {
            for (Identifier id : patient.getIdentifier()) {
                if (id.hasSystem() && id.getSystem().equals(patientSystem)) {
                    ssn = id.getValue();
                    break;
                }
            }
        }
        String fullName = "";
        if (patient.hasName() && !patient.getName().isEmpty()) {
            fullName = patient.getNameFirstRep().getNameAsSingleString();
        }
        Enumerations.AdministrativeGender administrativeGender = patient.getGender();
        Gender gender = null;
        if (administrativeGender != null) {
            gender =
                    switch (administrativeGender) {
                        case FEMALE -> Gender.Female;
                        case MALE -> Gender.Male;
                        case UNKNOWN -> Gender.Unknown;
                        case OTHER -> Gender.Other;
                        case NULL -> null;
                    };
        }
        String email = "";
        String phone = "";
        if (patient.hasTelecom()) {
            for (ContactPoint contactPoint : patient.getTelecom()) {
                if (phone.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    phone = contactPoint.getValue();
                }
                if (email.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    email = contactPoint.getValue();
                }
            }
        }
        String line = "";
        String city = "";
        String postalCode = "";
        if (patient.hasAddress() && !patient.getAddress().isEmpty()) {
            Address address = patient.getAddressFirstRep();
            if (address.hasLine() && !address.getLine().isEmpty()) {
                line = address.getLine().get(0).getValue();
            }
            if (address.hasCity()) {
                city = address.getCity();
            }
            if (address.hasPostalCode()) {
                postalCode = address.getPostalCode();
            }
        }

        return new PatientData(ssn, fullName, gender, email, phone, line, city, postalCode);
    }

    public List<Practitioner> getPractitionersByIdentifierSystemAndQuery(String name) {
        Bundle bundle = client.search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.hasSystemWithAnyCode(practitionerSystem))
                .where(Practitioner.NAME.contains().value(name))
                .sort().ascending(Practitioner.NAME)
                .returnBundle(Bundle.class)
                .execute();

        List<Practitioner> practitioners = new ArrayList<>(bundle.getEntry().stream()
                .map(p -> (Practitioner) p.getResource())
                .toList());

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            practitioners.addAll(bundle.getEntry().stream()
                    .map(p -> (Practitioner) p.getResource())
                    .toList());
        }
        return practitioners;
    }

    public PractitionerData getPractitionerData(Practitioner practitioner) {
        if (practitioner == null) {
            return null;
        }

        String hsaId = "";
        for (Identifier id : practitioner.getIdentifier()) {
            if (id.getSystem().equals(practitionerSystem)) {
                hsaId = id.getValue();
                break;
            }
        }

        String phone = "";
        String email = "";
        if (practitioner.hasTelecom()) {
            for (ContactPoint contactPoint : practitioner.getTelecom()) {
                if (phone.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    phone = contactPoint.getValue();
                }
                if (email.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    email = contactPoint.getValue();
                }
            }
        }

        String fullName = practitioner.getName().get(0).getNameAsSingleString();

        return new PractitionerData(hsaId, fullName, email, phone);
    }
}