package cloudos.service;

import cloudos.model.auth.AuthenticationException;
import cloudos.model.support.AccountRequest;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.cobbzilla.util.system.Command;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static cloudos.model.auth.AuthenticationException.Problem.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Service @Slf4j
public class KerberosService {

    public static final String KADMIN_USER = "kadmin/admin";

    @Autowired private CloudOsConfiguration configuration;

    // kerberizes an existing ldap account ... this assumes we've already created the user in the ldap directory
    public CommandResult createPrincipal (AccountRequest request) {
        final String accountDN = "uid=" + request.getName() + ",ou=People," + configuration.getLdap().getBaseDN();
        final String input = configuration.getKadminPassword()+ "\n" +
                "addprinc -x dn=\"" + accountDN + "\" " + request.getName() + "\n" +
                request.getPassword() + "\n" +
                request.getPassword() + "\n";

        // Create kerberos user
        CommandResult result = run_kadmin(input, "create");
        if (result.getStderr().contains("already exists")) {
            if (!request.isAdmin()) {
                throw new SimpleViolationException("{error.createAccount.alreadyExists}", "Account already exists", request.getName());
            }
            run_kadmin(configuration.getKadminPassword() + "\ndelprinc " + request.getName() + "\nyes\n", "delete");
            result = run_kadmin(input, "create");
        }
        return result;
    }

    public void authenticate(String accountName, String password) throws AuthenticationException {
        // verify kerberos login locally with loginRequest
        CommandLine kinit = new CommandLine("kinit").addArgument(accountName);

        long start = System.currentTimeMillis();
        CommandResult result;
        try {
            result = CommandShell.exec(new Command(kinit).setInput(password));
        } catch (Exception e) {
            log.error("error running kinit ("+e.toString()+"): " + e, e);
            throw new AuthenticationException(BOOTCONFIG_ERROR);
        } finally {
            log.info("kinit executed in "+ TimeUtil.formatDurationFrom(start));
        }

        if (!result.isZeroExitStatus()) throw new AuthenticationException(NOT_FOUND);

        try {
            result = CommandShell.exec("kdestroy");
            if (!result.isZeroExitStatus()) {
                log.warn("kdestroy returned non-zero: "+result.getExitStatus());
            }
        } catch (Exception e) {
            log.error("error running kdestroy: " + e, e);
            throw new AuthenticationException(BOOTCONFIG_ERROR);
        }
    }

    public void changePassword(String accountName, String oldPassword, String newPassword) throws AuthenticationException {

        final CommandLine command = new CommandLine("kpasswd").addArgument(accountName);
        final CommandResult result;
        try {
            result = CommandShell.exec(new Command(command).setInput(oldPassword+"\n"+newPassword+"\n"+newPassword+"\n"));
        } catch (Exception e) {
            log.error("changePassword: error running kpasswd: " + e, e);
            throw new AuthenticationException(INVALID);
        }

        if (!result.isZeroExitStatus()) {
            log.error("changePassword: kpasswd returned non-zero: "+result.getExitStatus());
            throw new IllegalArgumentException(result.getStderr());
        }
    }

    public void adminChangePassword(String accountName, String newPassword) {
        run_kadmin(configuration.getKadminPassword()+"\nchange_password "+accountName+"\n"+newPassword+"\n"+newPassword+"\n", "adminChangePassword");
    }

    // deprecated: deletion of kerberos principals is handled through the ldap service
    public void deletePrincipal(String accountName) {
        final String input = configuration.getKadminPassword()+"\ndelprinc "+ accountName + "\nyes\n";
        final CommandResult result = run_kadmin(input, "delete");
    }

    private CommandResult run_kadmin(String input, String method) {
        final CommandLine createPrincipalCommand = new CommandLine("kadmin").addArgument("-p").addArgument(KADMIN_USER);
        final CommandResult result;
        try {
            result = CommandShell.exec(new Command(createPrincipalCommand).setInput(input));
        } catch (Exception e) {
            return die(method + ": error running kadmin: " + e, e);
        }

        if (!result.isZeroExitStatus()) {
            log.error(method + ": kadmin returned non-zero: "+result.getExitStatus());
            die(result.getStderr());
        }

        return result;
    }
}
