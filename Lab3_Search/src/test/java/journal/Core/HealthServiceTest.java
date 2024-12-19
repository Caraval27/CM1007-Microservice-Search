package journal.Core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import io.quarkus.test.junit.QuarkusTest;
import journal.Core.Model.PatientData;
import journal.Core.Model.PractitionerData;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class HealthServiceTest {
    @InjectMocks
    private HealthService healthService;

    @Mock
    private FhirContext fhirContext;

    @Mock
    private IGenericClient mockClient;

    private static final String PATIENT_SYSTEM = "http://electronichealth.se/identifier/personnummer";
    private static final String PRACTITIONER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
    private static final String PRACTITIONER_ROLE_SYSTEM = "http://terminology.hl7.org/CodeSystem/practitioner-role";
    private static final String CONDITION_SYSTEM = "http://snomed.info/sct";

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(fhirContext.newRestfulGenericClient(anyString())).thenReturn(mockClient);
    }

    @Test
    void testGetPatientsByName() {
        String name = "John Doe";
        String firstPatientId = "1234";
        String secondPatientId = "5678";

        Patient mockPatient1 = createMockPatient(firstPatientId, "John Doe");
        Patient mockPatient2 = createMockPatient(secondPatientId, "Johnny Doe");

        Bundle mockBundle = new Bundle();
        Bundle.BundleEntryComponent entry1 = new Bundle.BundleEntryComponent();
        entry1.setResource(mockPatient1);
        Bundle.BundleEntryComponent entry2 = new Bundle.BundleEntryComponent();
        entry2.setResource(mockPatient2);
        mockBundle.addEntry(entry1);
        mockBundle.addEntry(entry2);

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery<Bundle> mockQueryForResource = mock(IQuery.class);
        IQuery<Bundle> mockWhere1 = mock(IQuery.class);
        IQuery<Bundle> mockWhere2 = mock(IQuery.class);
        ISort<Bundle> mockSort = mock(ISort.class);
        IQuery<Bundle> mockAscending = mock(IQuery.class);
        IQuery<Bundle> mockReturnBundle = mock(IQuery.class);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Patient.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where(any(ICriterion.class))).thenReturn(mockWhere1);
        when(mockWhere1.where(any(ICriterion.class))).thenReturn(mockWhere2);
        when(mockWhere2.sort()).thenReturn(mockSort);
        when(mockSort.ascending(any(IParam.class))).thenReturn(mockAscending);
        when(mockAscending.returnBundle(Bundle.class)).thenReturn(mockReturnBundle);
        when(mockReturnBundle.execute()).thenReturn(mockBundle);

        List<Patient> patients = new ArrayList<>();
        healthService.getPatientsByName(name)
                .subscribe()
                .with(patients::add);

        assertNotNull(patients, "Patients should not be null");
        assertEquals(2, patients.size(), "There should be two patients");
        assertEquals(firstPatientId, patients.get(0).getIdentifierFirstRep().getValue(), "First patient identifier value should match");
        assertEquals("John", patients.get(0).getNameFirstRep().getGivenAsSingleString(), "First patient given name should match");
        assertEquals(secondPatientId, patients.get(1).getIdentifierFirstRep().getValue(), "Second patient identifier value should match");
        assertEquals("Johnny", patients.get(1).getNameFirstRep().getGivenAsSingleString(), "Second patient given name should match");
    }

    @Test
    void testGetPatientsByNameAndPractitionerIdentifier() throws Exception {
        String name = "John Doe";
        String practitionerIdentifier = "12345";
        String firstPatientId = "1234";
        String secondPatientId = "5678";

        Practitioner mockPractitioner = createMockPractitioner(practitionerIdentifier, "Samantha Smith");
        HealthService healthService = Mockito.spy(new HealthService());
        Mockito.doReturn(mockPractitioner).when(healthService).getPractitionerByIdentifier(practitionerIdentifier);

        Patient mockPatient1 = createMockPatient(firstPatientId, "John Doe");
        Patient mockPatient2 = createMockPatient(secondPatientId, "Johnny Doe");

        Bundle mockBundle = new Bundle();
        Bundle.BundleEntryComponent entry1 = new Bundle.BundleEntryComponent();
        entry1.setResource(mockPatient1);
        Bundle.BundleEntryComponent entry2 = new Bundle.BundleEntryComponent();
        entry2.setResource(mockPatient2);
        mockBundle.addEntry(entry1);
        mockBundle.addEntry(entry2);

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery<Bundle> mockQueryForResource = mock(IQuery.class);
        IQuery<Bundle> mockWhere1 = mock(IQuery.class);
        IQuery<Bundle> mockWhere2 = mock(IQuery.class);
        IQuery<Bundle> mockWhere3 = mock(IQuery.class);
        ISort<Bundle> mockSort = mock(ISort.class);
        IQuery<Bundle> mockAscending = mock(IQuery.class);
        IQuery<Bundle> mockReturnBundle = mock(IQuery.class);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Patient.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where(any(ICriterion.class))).thenReturn(mockWhere1);
        when(mockWhere1.where(any(ICriterion.class))).thenReturn(mockWhere2);
        when(mockWhere2.where(any(ICriterion.class))).thenReturn(mockWhere3);
        when(mockWhere3.sort()).thenReturn(mockSort);
        when(mockSort.ascending(any(IParam.class))).thenReturn(mockAscending);
        when(mockAscending.returnBundle(Bundle.class)).thenReturn(mockReturnBundle);
        when(mockReturnBundle.execute()).thenReturn(mockBundle);

        Field clientField = HealthService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(healthService, mockClient);

        List<Patient> patients = new ArrayList<>();
        healthService.getPatientsByNameAndPractitionerIdentifier(name, practitionerIdentifier)
                .subscribe()
                .with(patients::add);

        assertNotNull(patients, "Patients should not be null");
        assertEquals(2, patients.size(), "There should be two patients");
        assertEquals(firstPatientId, patients.get(0).getIdentifierFirstRep().getValue(), "First patient identifier value should match");
        assertEquals("John", patients.get(0).getNameFirstRep().getGivenAsSingleString(), "First patient given name should match");
        assertEquals(secondPatientId, patients.get(1).getIdentifierFirstRep().getValue(), "Second patient identifier value should match");
        assertEquals("Johnny", patients.get(1).getNameFirstRep().getGivenAsSingleString(), "Second patient given name should match");
    }

    @Test
    void testGetPatientsByConditionCode() {
        String code = "112233";
        String patientId = "12345";

        Patient mockPatient = createMockPatient(patientId, "John Doe");

        Condition mockCondition = new Condition();
        mockCondition.setId("condition123");
        mockCondition.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem(CONDITION_SYSTEM).setCode(code)));
        mockCondition.setSubject(new Reference("Patient/" + patientId));

        Bundle mockBundle = new Bundle();
        Bundle.BundleEntryComponent conditionEntry = new Bundle.BundleEntryComponent();
        conditionEntry.setResource(mockCondition);
        Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(mockPatient);
        mockBundle.addEntry(conditionEntry);
        mockBundle.addEntry(patientEntry);

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery<Bundle> mockQueryForResource = mock(IQuery.class);
        IQuery<Bundle> mockWhere = mock(IQuery.class);
        IQuery<Bundle> mockInclude = mock(IQuery.class);
        IQuery<Bundle> mockReturnBundle = mock(IQuery.class);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Condition.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where(any(ICriterion.class))).thenReturn(mockWhere);
        when(mockWhere.include(any())).thenReturn(mockInclude);
        when(mockInclude.returnBundle(Bundle.class)).thenReturn(mockReturnBundle);
        when(mockReturnBundle.execute()).thenReturn(mockBundle);

        List<Patient> patients = new ArrayList<>();
        healthService.getPatientsByConditionCode(code)
                .subscribe()
                .with(patients::add);

        assertNotNull(patients, "Patients should not be null");
        assertEquals(1, patients.size(), "There should be one patient");
        assertEquals(patientId, patients.get(0).getIdentifierFirstRep().getValue(), "Patient identifier should match");
        assertEquals("John", patients.get(0).getNameFirstRep().getGivenAsSingleString(), "Patient given name should match");
    }

    @Test
    void testGetPatientsByConditionCodeAndPractitionerIdentifier() throws Exception {
        String code = "112233";
        String practitionerIdentifier = "12345";
        String patientId = "patient123";

        Practitioner mockPractitioner = createMockPractitioner(practitionerIdentifier, "Samantha Smith");
        HealthService healthService = Mockito.spy(new HealthService());
        Mockito.doReturn(mockPractitioner).when(healthService).getPractitionerByIdentifier(practitionerIdentifier);

        Patient mockPatient = createMockPatient(patientId, "John Doe");

        Condition mockCondition = new Condition();
        mockCondition.setId("condition123");
        mockCondition.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem(CONDITION_SYSTEM).setCode(code)));
        mockCondition.setSubject(new Reference("Patient/" + patientId));

        Bundle mockBundle = new Bundle();
        Bundle.BundleEntryComponent conditionEntry = new Bundle.BundleEntryComponent();
        conditionEntry.setResource(mockCondition);
        Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(mockPatient);
        mockBundle.addEntry(conditionEntry);
        mockBundle.addEntry(patientEntry);

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery<Bundle> mockQueryForResource = mock(IQuery.class);
        IQuery<Bundle> mockWhere1 = mock(IQuery.class);
        IQuery<Bundle> mockWhere2 = mock(IQuery.class);
        IQuery<Bundle> mockInclude = mock(IQuery.class);
        IQuery<Bundle> mockReturnBundle = mock(IQuery.class);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Condition.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where(any(ICriterion.class))).thenReturn(mockWhere1);
        when(mockWhere1.where(any(ICriterion.class))).thenReturn(mockWhere2);
        when(mockWhere2.include(any())).thenReturn(mockInclude);
        when(mockInclude.returnBundle(Bundle.class)).thenReturn(mockReturnBundle);
        when(mockReturnBundle.execute()).thenReturn(mockBundle);

        Field clientField = HealthService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(healthService, mockClient);

        List<Patient> patients = new ArrayList<>();
        healthService.getPatientsByConditionCodeAndPractitionerIdentifier(code, practitionerIdentifier)
                .subscribe()
                .with(patients::add);

        assertNotNull(patients, "Patients should not be null");
        assertEquals(1, patients.size(), "There should be one patient");
        assertEquals(patientId, patients.get(0).getIdentifierFirstRep().getValue(), "Patient identifier value should match");
        assertEquals("John", patients.get(0).getNameFirstRep().getGivenAsSingleString(), "Patient given name should match");
    }

    @Test
    void testGetPractitionerByIdentifier() {
        String identifierValue = "12345";

        Practitioner mockPractitioner = createMockPractitioner(identifierValue, "Jane Doe");

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery mockQueryForResource = mock(IQuery.class);
        IQuery mockWhere = mock(IQuery.class);
        Bundle mockBundle = new Bundle();
        if (mockPractitioner != null) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            entry.setResource(mockPractitioner);
            mockBundle.addEntry(entry);
        }

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Practitioner.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where((ICriterion<?>) any())).thenReturn(mockWhere);
        when(mockWhere.returnBundle(Bundle.class)).thenReturn(mockWhere);
        when(mockWhere.execute()).thenReturn(mockBundle);

        Practitioner practitioner = healthService.getPractitionerByIdentifier(identifierValue);

        assertNotNull(practitioner, "Patient should not be null");
        assertEquals(identifierValue, practitioner.getIdentifierFirstRep().getValue(), "Practitioner identifier value should match input value");
    }

    @Test
    void testGetPractitionerByName() {
        String name = "John Doe";
        String firstPractitionerId = "1234";
        String secondPractitionerId = "5678";

        Practitioner mockPractitioner1 = createMockPractitioner(firstPractitionerId, "John Doe");
        Practitioner mockPractitioner2 = createMockPractitioner(secondPractitionerId, "Johnny Doe");

        Bundle mockBundle = new Bundle();
        Bundle.BundleEntryComponent entry1 = new Bundle.BundleEntryComponent();
        entry1.setResource(mockPractitioner1);
        Bundle.BundleEntryComponent entry2 = new Bundle.BundleEntryComponent();
        entry2.setResource(mockPractitioner2);
        mockBundle.addEntry(entry1);
        mockBundle.addEntry(entry2);

        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery<Bundle> mockQueryForResource = mock(IQuery.class);
        IQuery<Bundle> mockWhere1 = mock(IQuery.class);
        IQuery<Bundle> mockWhere2 = mock(IQuery.class);
        ISort<Bundle> mockSort = mock(ISort.class);
        IQuery<Bundle> mockAscending = mock(IQuery.class);
        IQuery<Bundle> mockReturnBundle = mock(IQuery.class);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(Practitioner.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where(any(ICriterion.class))).thenReturn(mockWhere1);
        when(mockWhere1.where(any(ICriterion.class))).thenReturn(mockWhere2);
        when(mockWhere2.sort()).thenReturn(mockSort);
        when(mockSort.ascending(any(IParam.class))).thenReturn(mockAscending);
        when(mockAscending.returnBundle(Bundle.class)).thenReturn(mockReturnBundle);
        when(mockReturnBundle.execute()).thenReturn(mockBundle);

        List<Practitioner> practitioners = new ArrayList<>();
        healthService.getPractitionersByName(name)
                .subscribe()
                .with(practitioners::add);

        assertNotNull(practitioners, "Practitioners should not be null");
        assertEquals(2, practitioners.size(), "There should be two practitioners");
        assertEquals(firstPractitionerId, practitioners.get(0).getIdentifierFirstRep().getValue(), "First practitioner identifier value should match");
        assertEquals("John", practitioners.get(0).getNameFirstRep().getGivenAsSingleString(), "First practitioner given name should match");
        assertEquals(secondPractitionerId, practitioners.get(1).getIdentifierFirstRep().getValue(), "Second practitioner identifier value should match");
        assertEquals("Johnny", practitioners.get(1).getNameFirstRep().getGivenAsSingleString(), "Second practitioner given name should match");
    }

    @Test
    void testGetPatientData() {
        String identifierValue = "12345";
        String patientName = "John Doe";

        Patient mockPatient = createMockPatient(identifierValue, patientName);
        mockPatient.setGender(Enumerations.AdministrativeGender.FEMALE);

        PatientData patientData = healthService.getPatientData(mockPatient);

        assertNotNull(patientData, "Patient data should not be null");
        assertEquals(identifierValue, patientData.getId(), "Patient identifier should match input value");
        assertEquals(patientName, patientData.getFullName(), "Patient full name should match");
        assertEquals("Female", patientData.getGender().name(), "Patient gender should match");
    }

    @Test
    void testGetPractitionerData() {
        String hsaId = "12345";
        String phone = "555-1234";
        String email = "test@example.com";
        String fullName = "John Doe";
        String roleDisplay = "Doctor";

        Practitioner mockPractitioner = createMockPractitioner(hsaId, fullName);
        mockPractitioner.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(phone);
        mockPractitioner.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                .setValue(email);

        PractitionerRole mockPractitionerRole = new PractitionerRole();
        CodeableConcept mockCodeableConcept = new CodeableConcept();
        mockCodeableConcept.addCoding(new Coding()
                .setSystem(PRACTITIONER_ROLE_SYSTEM)
                .setDisplay(roleDisplay));
        mockPractitionerRole.addCode(mockCodeableConcept);

        mockPractitionerRoleSearch(mockPractitionerRole);

        PractitionerData practitionerData = healthService.getPractitionerData(mockPractitioner);

        assertNotNull(practitionerData, "PractitionerData should not be null");
        assertEquals(hsaId, practitionerData.getId(), "HSA ID should match");
        assertEquals(phone, practitionerData.getPhone(), "Phone number should match");
        assertEquals(email, practitionerData.getEmail(), "Email should match");
        assertEquals(fullName, practitionerData.getFullName(), "Full name should match");
        assertEquals(roleDisplay, practitionerData.getRole(), "Role should match");
    }

    @Test
    void testGetPractitionerRoleByPractitionerId() {
        String practitionerId = "12345";
        String roleDisplay = "Doctor";

        PractitionerRole mockPractitionerRole = new PractitionerRole();
        CodeableConcept mockCodeableConcept = new CodeableConcept();
        mockCodeableConcept.addCoding(new Coding()
                .setSystem(PRACTITIONER_ROLE_SYSTEM)
                .setDisplay(roleDisplay));
        mockPractitionerRole.addCode(mockCodeableConcept);

        mockPractitionerRoleSearch(mockPractitionerRole);

        List<PractitionerRole> practitionerRoles = healthService.getPractitionerRoleByPractitionerId(practitionerId);

        assertNotNull(practitionerRoles, "Result should not be null");
        assertEquals(1, practitionerRoles.size(), "Result should contain one PractitionerRole");
        assertEquals(roleDisplay, practitionerRoles.get(0).getCodeFirstRep().getCodingFirstRep().getDisplay(), "Role display should match");
    }

    private Patient createMockPatient(String identifierValue, String fullName) {
        Patient mockPatient = new Patient();
        mockPatient.addIdentifier().setSystem(PATIENT_SYSTEM).setValue(identifierValue);
        mockPatient.addName().setFamily(fullName.split(" ")[1]).addGiven(fullName.split(" ")[0]);
        return mockPatient;
    }

    private Practitioner createMockPractitioner(String identifierValue, String fullName) {
        Practitioner mockPractitioner = new Practitioner();
        mockPractitioner.addIdentifier().setSystem(PRACTITIONER_SYSTEM).setValue(identifierValue);
        mockPractitioner.addName().setFamily(fullName.split(" ")[1]).addGiven(fullName.split(" ")[0]);
        return mockPractitioner;
    }

    private void mockPractitionerRoleSearch(PractitionerRole mockPractitionerRole) {
        IUntypedQuery mockQuery = mock(IUntypedQuery.class);
        IQuery mockQueryForResource = mock(IQuery.class);
        IQuery mockWhere = mock(IQuery.class);
        Bundle mockBundle = new Bundle();

        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(mockPractitionerRole);
        mockBundle.addEntry(entry);

        when(mockClient.search()).thenReturn(mockQuery);
        when(mockQuery.forResource(PractitionerRole.class)).thenReturn(mockQueryForResource);
        when(mockQueryForResource.where((ICriterion<?>) any())).thenReturn(mockWhere);
        when(mockWhere.returnBundle(Bundle.class)).thenReturn(mockWhere);
        when(mockWhere.execute()).thenReturn(mockBundle);
    }
}