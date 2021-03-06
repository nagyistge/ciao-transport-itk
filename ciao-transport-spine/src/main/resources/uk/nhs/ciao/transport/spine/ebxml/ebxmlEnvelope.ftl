<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
	xmlns:eb="http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"<#if manifest>
	xmlns:hl7ebxml="urn:hl7-org:transport/ebxml/DSTUv1.0"
	xmlns:xlink="http://www.w3.org/1999/xlink"</#if>>
	<soap:Header>
		<eb:MessageHeader eb:version="2.0" soap:mustUnderstand="1">
			<#if fromParty??>
			<eb:From>
				<eb:PartyId eb:type="urn:nhs:names:partyType:ocs+serviceInstance">${fromParty?xml}</eb:PartyId>
			</eb:From>
			</#if>
			<#if toParty??>
			<eb:To>
				<eb:PartyId eb:type="urn:nhs:names:partyType:ocs+serviceInstance">${toParty?xml}</eb:PartyId>
			</eb:To>
			</#if>
			
			<#if cpaId??>
			<eb:CPAId>${cpaId?xml}</eb:CPAId>
			</#if>
			<#if conversationId??>
			<eb:ConversationId>${conversationId?xml}</eb:ConversationId>
			</#if>
			<#if service??>
			<eb:Service>${service?xml}</eb:Service>
			</#if>
			<#if action??>
			<eb:Action>${action?xml}</eb:Action>
			</#if>

			<eb:MessageData>
				<#if messageData.messageId??>
				<eb:MessageId>${messageData.messageId?xml}</eb:MessageId>
				</#if>
				<#if messageData.timestamp??>
				<eb:Timestamp>${messageData.timestamp?xml}</eb:Timestamp>
				</#if>
				<#if messageData.refToMessageId??>
				<eb:RefToMessageId>${messageData.refToMessageId?xml}</eb:RefToMessageId>
				</#if>
			</eb:MessageData>
			<#if duplicateElimination>
			
			<eb:DuplicateElimination />
			</#if>
		</eb:MessageHeader>
		<#if ackRequested>
		<eb:AckRequested eb:version="2.0" soap:mustUnderstand="1" soap:actor="urn:oasis:names:tc:ebxml-msg:actor:toPartyMSH" eb:signed="false"/>
		</#if>
		<#if acknowledgment>
		<eb:Acknowledgment eb:version="2.0" soap:mustUnderstand="1" soap:actor="urn:oasis:names:tc:ebxml-msg:actor:toPartyMSH">
			<#if messageData.timestamp??>
			<eb:Timestamp>${messageData.timestamp?xml}</eb:Timestamp>
			</#if>
			<#if messageData.refToMessageId??>
			<eb:RefToMessageId>${messageData.refToMessageId?xml}</eb:RefToMessageId>
			</#if>
			<#if fromParty??>
			<eb:From>
				<eb:PartyId eb:type="urn:nhs:names:partyType:ocs+serviceInstance">${fromParty?xml}</eb:PartyId>
			</eb:From>
			</#if>
		</eb:Acknowledgment>
		</#if>
		<#if errorMessage>
		<eb:ErrorList <#if error.listId??>eb:id="${error.listId?xml}"</#if> <#if error.severity??>eb:highestSeverity="${error.severity?xml}"</#if> eb:version="2.0" soap:mustUnderstand="1">
			<eb:Error <#if error.id??>eb:id="${error.id?xml}"</#if> <#if error.code??>eb:errorCode="${error.code?xml}"</#if> <#if error.severity??>eb:severity="${error.severity?xml}"</#if> <#if error.codeContext??>eb:codeContext="${error.codeContext?xml}"</#if>>
				<#if error.description??>
				<eb:Description xml:lang="en-GB">${error.description?xml}</eb:Description>
				</#if>
			</eb:Error>
		</eb:ErrorList>
		</#if>
	</soap:Header>
	<#if manifest>
	<soap:Body>
		<eb:Manifest eb:version="2.0">
			<#list manifestReferences as reference>
			<eb:Reference <#if reference.href??>xlink:href="${reference.href?xml}"</#if>>
				<#if reference.hl7>
				<eb:Schema eb:location="http://www.nhsia.nhs.uk/schemas/HL7-Message.xsd" eb:version="1.0"/>
				</#if>
				<#if reference.description??>
				<eb:Description xml:lang="en">${reference.description?xml}</eb:Description>
				</#if>
				<#if reference.hl7>
				<hl7ebxml:Payload style="HL7" encoding="XML" version="3.0"/>
				</#if>
			</eb:Reference>
			</#list>
		</eb:Manifest>
	</soap:Body>
	<#else>
	<soap:Body />
	</#if>
</soap:Envelope>
