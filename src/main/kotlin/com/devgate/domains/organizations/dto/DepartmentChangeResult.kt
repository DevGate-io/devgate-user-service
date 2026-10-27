package com.devgate.domains.organizations.dto

import com.devgate.domains.users.models.Target

// Internal service result: the controller returns the refreshed organization tree and
// audits the affected department.
data class DepartmentChangeResult(
	val organization: OrganizationResponse,
	val department: Target
)