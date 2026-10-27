package com.devgate.users.services

import com.devgate.domains.organizations.dto.CreateDepartmentRequest
import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.models.Department
import com.devgate.domains.organizations.models.Organization
import com.devgate.domains.organizations.repositories.OrganizationRepository
import com.devgate.domains.organizations.services.impl.OrganizationServiceImpl
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class OrganizationServiceImplTest {
	@Mock
	private lateinit var organizationRepository: OrganizationRepository

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
}