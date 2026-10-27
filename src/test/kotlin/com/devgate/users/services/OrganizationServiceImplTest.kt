package com.devgate.users.services

import com.devgate.domains.organizations.dto.CreateDepartmentInOrganizationRequest
import com.devgate.domains.organizations.dto.CreateDepartmentRequest
import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.UpdateDepartmentRequest
import com.devgate.domains.organizations.models.Department
import com.devgate.domains.organizations.models.Organization
import com.devgate.domains.organizations.repositories.DepartmentRepository
import com.devgate.domains.organizations.repositories.OrganizationRepository
import com.devgate.domains.organizations.services.impl.OrganizationServiceImpl
import com.devgate.exceptions.ApiException
import com.devgate.exceptions.DepartmentDepthExceededException
import com.devgate.exceptions.DepartmentNotFoundException
import com.devgate.exceptions.OrganizationAlreadyExistsException
import com.devgate.exceptions.OrganizationNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.HttpStatus
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class OrganizationServiceImplTest {
	@Mock
	private lateinit var organizationRepository: OrganizationRepository

	@Mock
	private lateinit var departmentRepository: DepartmentRepository

	@InjectMocks
	private lateinit var organizationService: OrganizationServiceImpl

	// Mimics JPA persist: ids are generated for the organization and every cascaded department.
	private fun stubSaveAssigningIds() {
		`when`(organizationRepository.save(any(Organization::class.java))).thenAnswer { invocation ->
			val organization = invocation.getArgument<Organization>(0)
			organization.id = UUID.randomUUID()
			organization.departments.forEach { it.id = UUID.randomUUID() }
			organization
		}
	}

	@Test
	fun `createOrganization - builds nested department tree`() {
		val request =
			CreateOrganizationRequest(
				name = "Acme",
				description = "Main org",
				departments =
					listOf(
						CreateDepartmentRequest(
							name = "R&D",
							children =
								listOf(
									CreateDepartmentRequest(name = "Platform"),
									CreateDepartmentRequest(
										name = "Data",
										children = listOf(CreateDepartmentRequest(name = "ML"))
									)
								)
						),
						CreateDepartmentRequest(name = "Sales")
					)
			)

		`when`(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(false)
		stubSaveAssigningIds()

		val result = organizationService.createOrganization(request)

		assertEquals("Acme", result.name)
		assertEquals("Main org", result.description)
		assertEquals(listOf("R&D", "Sales"), result.departments.map { it.name })

		val rnd = result.departments[0]
		assertNull(rnd.parentId)
		assertEquals(listOf("Platform", "Data"), rnd.children.map { it.name })
		assertTrue(rnd.children.all { it.parentId == rnd.id })

		val data = rnd.children[1]
		assertEquals(listOf("ML"), data.children.map { it.name })
		assertEquals(data.id, data.children[0].parentId)
		assertTrue(result.departments[1].children.isEmpty())
	}

	@Test
	fun `createOrganization - attaches every department to the organization in pre-order`() {
		val request =
			CreateOrganizationRequest(
				name = "Acme",
				departments =
					listOf(
						CreateDepartmentRequest(
							name = "R&D",
							children = listOf(CreateDepartmentRequest(name = "Platform"))
						),
						CreateDepartmentRequest(name = "Sales")
					)
			)
		val captor = ArgumentCaptor.forClass(Organization::class.java)

		`when`(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(false)
		stubSaveAssigningIds()

		organizationService.createOrganization(request)

		verify(organizationRepository).save(captor.capture())
		val saved = captor.value

		assertEquals(listOf("R&D", "Platform", "Sales"), saved.departments.map { it.name })
		assertTrue(saved.departments.all { it.organization === saved })
		assertNull(saved.departments[0].parent)
		assertSame(saved.departments[0], saved.departments[1].parent)
		assertNull(saved.departments[2].parent)
	}

	@Test
	fun `createOrganization - trims fields and defaults description`() {
		val request =
			CreateOrganizationRequest(
				name = "  Acme  ",
				description = null,
				departments = listOf(CreateDepartmentRequest(name = " R&D ", description = " Research "))
			)

		`when`(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(false)
		stubSaveAssigningIds()

		val result = organizationService.createOrganization(request)

		assertEquals("Acme", result.name)
		assertEquals("", result.description)
		assertEquals("R&D", result.departments[0].name)
		assertEquals("Research", result.departments[0].description)
	}

	@Test
	fun `createOrganization - duplicate name`() {
		val request = CreateOrganizationRequest(name = "Acme")

		`when`(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(true)

		assertThrows(OrganizationAlreadyExistsException::class.java) {
			organizationService.createOrganization(request)
		}
		verify(organizationRepository, never()).save(any(Organization::class.java))
	}

	@Test
	fun `getOrganizationById - not found`() {
		val id = UUID.randomUUID()

		`when`(organizationRepository.findById(id)).thenReturn(Optional.empty())

		assertThrows(OrganizationNotFoundException::class.java) {
			organizationService.getOrganizationById(id)
		}
	}

	@Test
	fun `getAllOrganizations - maps flat departments into tree`() {
		val organization = Organization(id = UUID.randomUUID(), name = "Acme", description = "Main org")
		val rnd = Department(id = UUID.randomUUID(), organization = organization, name = "R&D")
		val platform = Department(id = UUID.randomUUID(), organization = organization, parent = rnd, name = "Platform")
		val sales = Department(id = UUID.randomUUID(), organization = organization, name = "Sales")
		organization.departments.addAll(listOf(rnd, platform, sales))

		`when`(organizationRepository.findAll()).thenReturn(listOf(organization))

		val result = organizationService.getAllOrganizations()

		assertEquals(1, result.size)
		assertEquals(listOf("R&D", "Sales"), result[0].departments.map { it.name })
		assertEquals(listOf(platform.id), result[0].departments[0].children.map { it.id })
		assertEquals(rnd.id, result[0].departments[0].children[0].parentId)
	}

	private val organizationId = UUID.randomUUID()

	private fun fakeOrganization(): Organization = Organization(id = organizationId, name = "Acme")

	private fun fakeDepartment(
		organization: Organization,
		name: String,
		parent: Department? = null
	): Department {
		val department = Department(id = UUID.randomUUID(), organization = organization, parent = parent, name = name)
		organization.departments.add(department)
		return department
	}

	// Mimics persist + flush: the id is assigned and the lazily loaded collection sees the row.
	private fun stubSaveAndFlushDepartment(organization: Organization) {
		`when`(departmentRepository.saveAndFlush(any(Department::class.java))).thenAnswer { invocation ->
			val department = invocation.getArgument<Department>(0)
			department.id = UUID.randomUUID()
			organization.departments.add(department)
			department
		}
	}

	@Test
	fun `createOrganization - rejects nesting deeper than max depth`() {
		val request =
			CreateOrganizationRequest(
				name = "Acme",
				departments =
					listOf(
						CreateDepartmentRequest(
							name = "L1",
							children =
								listOf(
									CreateDepartmentRequest(
										name = "L2",
										children =
											listOf(
												CreateDepartmentRequest(
													name = "L3",
													children = listOf(CreateDepartmentRequest(name = "L4"))
												)
											)
									)
								)
						)
					)
			)

		`when`(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(false)

		assertThrows(DepartmentDepthExceededException::class.java) {
			organizationService.createOrganization(request)
		}
		verify(organizationRepository, never()).save(any(Organization::class.java))
	}

	@Test
	fun `addDepartment - adds root department`() {
		val organization = fakeOrganization()

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		stubSaveAndFlushDepartment(organization)

		val result =
			organizationService.addDepartment(
				organizationId,
				CreateDepartmentInOrganizationRequest(name = "  R&D  ", description = " Research ")
			)

		assertEquals(listOf("R&D"), result.organization.departments.map { it.name })
		assertEquals("Research", result.organization.departments[0].description)
		assertNull(result.organization.departments[0].parentId)
		assertEquals("department", result.department.type)
		assertEquals("R&D", result.department.label)
		val created = result.organization.departments[0]
		assertEquals(created.id.toString(), result.department.id)
	}

	@Test
	fun `addDepartment - adds nested department under parent`() {
		val organization = fakeOrganization()
		val parent = fakeDepartment(organization, "R&D")
		val captor = ArgumentCaptor.forClass(Department::class.java)

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		`when`(departmentRepository.findByIdAndOrganizationId(parent.id!!, organizationId)).thenReturn(parent)
		stubSaveAndFlushDepartment(organization)

		val result =
			organizationService.addDepartment(
				organizationId,
				CreateDepartmentInOrganizationRequest(name = "Platform", parentId = parent.id)
			)

		verify(departmentRepository).saveAndFlush(captor.capture())
		assertSame(parent, captor.value.parent)
		assertSame(organization, captor.value.organization)
		val rnd = result.organization.departments[0]
		assertEquals(listOf("Platform"), rnd.children.map { it.name })
	}

	@Test
	fun `addDepartment - rejects nesting deeper than max depth`() {
		val organization = fakeOrganization()
		val level1 = fakeDepartment(organization, "L1")
		val level2 = fakeDepartment(organization, "L2", level1)
		val level3 = fakeDepartment(organization, "L3", level2)

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		`when`(departmentRepository.findByIdAndOrganizationId(level3.id!!, organizationId)).thenReturn(level3)

		assertThrows(DepartmentDepthExceededException::class.java) {
			organizationService.addDepartment(
				organizationId,
				CreateDepartmentInOrganizationRequest(name = "L4", parentId = level3.id)
			)
		}
		verify(departmentRepository, never()).saveAndFlush(any(Department::class.java))
	}

	@Test
	fun `addDepartment - parent from another organization`() {
		val organization = fakeOrganization()
		val foreignParentId = UUID.randomUUID()

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		`when`(departmentRepository.findByIdAndOrganizationId(foreignParentId, organizationId)).thenReturn(null)

		assertThrows(DepartmentNotFoundException::class.java) {
			organizationService.addDepartment(
				organizationId,
				CreateDepartmentInOrganizationRequest(name = "Platform", parentId = foreignParentId)
			)
		}
		verify(departmentRepository, never()).saveAndFlush(any(Department::class.java))
	}

	@Test
	fun `addDepartment - organization not found`() {
		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.empty())

		assertThrows(OrganizationNotFoundException::class.java) {
			organizationService.addDepartment(organizationId, CreateDepartmentInOrganizationRequest(name = "R&D"))
		}
	}

	@Test
	fun `updateDepartment - trims and applies fields`() {
		val organization = fakeOrganization()
		val department = fakeDepartment(organization, "R&D")

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		`when`(departmentRepository.findByIdAndOrganizationId(department.id!!, organizationId)).thenReturn(department)

		val result =
			organizationService.updateDepartment(
				organizationId,
				department.id!!,
				UpdateDepartmentRequest(name = "  Engineering  ", description = " Builds things ")
			)

		assertEquals("Engineering", department.name)
		assertEquals("Builds things", department.description)
		assertEquals("Engineering", result.organization.departments[0].name)
		assertEquals("Engineering", result.department.label)
	}

	@Test
	fun `updateDepartment - blank name`() {
		val organization = fakeOrganization()
		val department = fakeDepartment(organization, "R&D")

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization))
		`when`(departmentRepository.findByIdAndOrganizationId(department.id!!, organizationId)).thenReturn(department)

		val exception =
			assertThrows(ApiException::class.java) {
				organizationService.updateDepartment(organizationId, department.id!!, UpdateDepartmentRequest(name = "  "))
			}

		assertEquals(HttpStatus.BAD_REQUEST, exception.httpStatus)
		assertEquals("R&D", department.name)
	}

	@Test
	fun `updateDepartment - not found`() {
		val departmentId = UUID.randomUUID()

		`when`(organizationRepository.findById(organizationId)).thenReturn(Optional.of(fakeOrganization()))
		`when`(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)).thenReturn(null)

		assertThrows(DepartmentNotFoundException::class.java) {
			organizationService.updateDepartment(organizationId, departmentId, UpdateDepartmentRequest(name = "X"))
		}
	}

	@Test
	fun `deleteDepartment - deletes and returns audit target`() {
		val organization = fakeOrganization()
		val department = fakeDepartment(organization, "R&D")

		`when`(departmentRepository.findByIdAndOrganizationId(department.id!!, organizationId)).thenReturn(department)

		val target = organizationService.deleteDepartment(organizationId, department.id!!)

		verify(departmentRepository).delete(department)
		assertEquals("R&D", target.label)
		assertEquals("department", target.type)
		assertEquals(department.id.toString(), target.id)
	}

	@Test
	fun `deleteDepartment - not found`() {
		val departmentId = UUID.randomUUID()

		`when`(departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)).thenReturn(null)

		assertThrows(DepartmentNotFoundException::class.java) {
			organizationService.deleteDepartment(organizationId, departmentId)
		}
		verify(departmentRepository, never()).delete(any(Department::class.java))
	}
}