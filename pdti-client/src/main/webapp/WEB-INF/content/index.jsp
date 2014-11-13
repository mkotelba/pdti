<%@ page contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html>
    <%@ include file="head.jsp"%>
    <body>
        <div class="container">
            <h3><s:text name="searchLegend" /></h3>
            <hr/>
            <s:form action="search" validate="true" class="form-inline">
                <fieldset>
                    <div style="display:block; text-align:center">
                        <h5><s:text name="defaultUrl"/>: <s:text name="provider.directory.wsdl.url"/></h5>
                        <s:textfield style="width: 400px;" key="url"/>
                        <!-- <s:hidden name="providerDirectoryType" value="HPDPlus WSDL"/> replaced this with following line.av-->
                        <s:select style="width: 415px;" name="providerDirectoryType" value="HPDPlus WSDL" key="providerDirectoryType" list="@gov.hhs.onc.pdti.client.types.ProviderDirectoryTypes@values()"/>
                        <s:textfield style="width: 400px;" key="textDN"/>
                        <s:textfield style="width: 400px;" key="searchAttribute"/>
                        <s:textfield style="width: 400px;" key="searchString" />
                        <s:submit />
                    </div>
                </fieldset>
            </s:form>
        </div>
    </body>
</html>