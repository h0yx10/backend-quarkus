package com.events.infrastructure.utils.constants;

public final class MessageConstants {

    public static final String EVENTO_CREATED = "El evento se creo correctamente.";
    public static final String EVENTO_RETRIEVED = "El evento se consulto correctamente.";
    public static final String EVENTO_LIST_RETRIEVED = "Los eventos se consultaron correctamente.";
    public static final String EVENTO_UPDATED = "El evento se actualizo correctamente.";
    public static final String EVENTO_DELETED = "El evento se elimino correctamente.";
    public static final String EVENTO_NOT_FOUND = "No encontramos el evento solicitado.";
    public static final String EVENTO_PROGRESS_RETRIEVED = "El progreso del evento se consulto correctamente.";

    public static final String SUBTAREA_CREATED = "La subtarea se creo correctamente.";
    public static final String SUBTAREA_LIST_RETRIEVED = "Las subtareas se consultaron correctamente.";
    public static final String SUBTAREA_UPDATED = "La subtarea se actualizo correctamente.";
    public static final String SUBTAREA_STATUS_UPDATED = "El estado de la subtarea se actualizo correctamente.";
    public static final String SUBTAREA_DELETED = "La subtarea se elimino correctamente.";
    public static final String SUBTAREA_NOT_FOUND = "No encontramos la subtarea solicitada.";

    public static final String TODAY_RETRIEVED = "La vista Hoy se consulto correctamente.";
    public static final String OVERLOAD_CHECK_RETRIEVED = "La verificacion de sobrecarga se realizo correctamente.";

    public static final String CAPACIDAD_RETRIEVED = "La capacidad diaria se consulto correctamente.";
    public static final String CAPACIDAD_UPDATED = "La capacidad diaria se actualizo correctamente.";

    public static final String REGISTER_SUCCESS = "La cuenta se creo correctamente.";
    public static final String LOGOUT_SUCCESS = "Cerraste sesion correctamente.";
    public static final String LOGIN_SUCCESS = "Iniciaste sesion correctamente.";
    public static final String CURRENT_USER_RETRIEVED = "El usuario se consulto correctamente.";
    public static final String USUARIO_LIST_RETRIEVED = "Los usuarios se consultaron correctamente.";

    public static final String NOMBRE_REQUIRED = "Escribe un nombre.";
    public static final String NOMBRE_USUARIO_MAX_LENGTH = "El nombre puede tener maximo 120 caracteres.";
    public static final String CORREO_REQUIRED = "Escribe tu correo.";
    public static final String CORREO_INVALID = "Escribe un correo valido.";
    public static final String PASSWORD_REQUIRED = "Escribe tu contrasena.";
    public static final String PASSWORD_LENGTH = "La contrasena debe tener al menos 8 caracteres y un maximo de 72 bytes UTF-8.";
    public static final String NOMBRE_MAX_LENGTH = "El nombre puede tener maximo 180 caracteres.";
    public static final String TIPO_REQUIRED = "Selecciona un tipo de evento.";
    public static final String FECHA_HORA_REQUIRED = "Indica la fecha y hora del evento.";
    public static final String FECHA_OBJETIVO_REQUIRED = "Indica el plazo de la subtarea.";
    public static final String HORAS_ESTIMADAS_REQUIRED = "Indica las horas estimadas.";
    public static final String HORAS_ESTIMADAS_POSITIVE = "Las horas estimadas deben ser mayores a 0.";
    public static final String ESTADO_REQUIRED = "Selecciona un estado.";
    public static final String LIMITE_HORAS_REQUIRED = "Indica el limite de horas diario.";

    public static final String ORGANIZADOR_NOT_FOUND = "No encontramos el usuario autenticado.";
    public static final String UNAUTHENTICATED = "Debes iniciar sesion para acceder a este recurso.";
    public static final String INVALID_REQUEST = "Revisa los datos ingresados e intentalo nuevamente.";
    public static final String UNEXPECTED_ERROR = "Ocurrio un inconveniente. Intentalo nuevamente mas tarde.";

    private MessageConstants() {
    }
}
