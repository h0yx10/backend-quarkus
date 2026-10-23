package com.events.infrastructure.config;

import com.events.application.port.in.GetCurrentUserPort;
import com.events.application.port.in.ListUsuariosPort;
import com.events.application.port.in.LoginPort;
import com.events.application.port.in.RegisterPort;
import com.events.application.port.out.PasswordHasherPort;
import com.events.application.port.out.PlanningDataPort;
import com.events.application.port.out.RolRepositoryPort;
import com.events.application.port.out.TokenProviderPort;
import com.events.application.port.out.UsuarioRepositoryPort;
import com.events.application.usecase.GetCurrentUserUseCase;
import com.events.application.usecase.ListUsuariosUseCase;
import com.events.application.usecase.LoginUseCase;
import com.events.application.usecase.RegisterUseCase;
import com.events.application.port.out.CurrentUsuarioPort;
import com.events.application.port.out.TransactionPort;
import com.events.application.port.in.UsuariosPort;
import com.events.application.usecase.UsuariosUseCase;
import com.events.application.port.in.LogoutPort;
import com.events.application.port.out.CurrentTokenPort;
import com.events.application.port.out.TokenRevocationPort;
import com.events.application.usecase.LogoutUseCase;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UseCaseConfig {

    @Produces
    @ApplicationScoped
    public RegisterPort registerPort(UsuarioRepositoryPort usuarioRepository,
                                     RolRepositoryPort rolRepository,
                                     PasswordHasherPort passwordHasher,
                                     TokenProviderPort tokenProvider, TransactionPort transaction) {
        return new RegisterUseCase(usuarioRepository, rolRepository, passwordHasher, tokenProvider, transaction);
    }

    @Produces
    @ApplicationScoped
    public LogoutPort logoutPort(CurrentTokenPort current, TokenRevocationPort revocations) {
        return new LogoutUseCase(current, revocations);
    }

    @Produces
    @ApplicationScoped
    public LoginPort loginPort(UsuarioRepositoryPort usuarioRepository,
                               PasswordHasherPort passwordHasher,
                               TokenProviderPort tokenProvider) {
        return new LoginUseCase(usuarioRepository, passwordHasher, tokenProvider);
    }

    @Produces
    @ApplicationScoped
    public GetCurrentUserPort getCurrentUserPort(UsuarioRepositoryPort usuarioRepository,
                                                 CurrentUsuarioPort currentUsuario) {
        return new GetCurrentUserUseCase(usuarioRepository, currentUsuario);
    }

    @Produces
    @ApplicationScoped
    public UsuariosPort usuariosPort(UsuarioRepositoryPort usuarios, RolRepositoryPort roles,
                                    PasswordHasherPort passwords, CurrentUsuarioPort current, TransactionPort transaction,
                                    PlanningDataPort planning) {
        return new UsuariosUseCase(usuarios, roles, passwords, current, transaction, planning);
    }

    @Produces
    @ApplicationScoped
    public ListUsuariosPort listUsuariosPort(UsuarioRepositoryPort usuarioRepository) {
        return new ListUsuariosUseCase(usuarioRepository);
    }

}
