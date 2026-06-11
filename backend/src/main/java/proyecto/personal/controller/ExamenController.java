package proyecto.personal.controller;

import proyecto.personal.model.Curso;
import proyecto.personal.model.Usuario;
import proyecto.personal.service.ExamenService;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.UsuarioService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/examenes")
public class ExamenController {

    private final ExamenService examenService;
    private final CursoService cursoService;
    private final UsuarioService usuarioService;

    public ExamenController(
            ExamenService examenService,
            CursoService cursoService,
            UsuarioService usuarioService) {

        this.examenService = examenService;
        this.cursoService = cursoService;
        this.usuarioService = usuarioService;
    }

    // =========================
    // VISTA PRINCIPAL
    // =========================

    @GetMapping
    public String verExamenes(
            @RequestParam(required = false) Long cursoId,
            Model model) {

        List<Curso> cursos = cursoService.listarCursosDelUsuarioActual();

        cursos.sort(Comparator.comparing(Curso::getNombre));

        model.addAttribute("cursos", cursos);

        if (cursos.isEmpty()) {
            return "examenes";
        }

        if (cursoId == null) {
            cursoId = cursos.get(0).getId();
        }

        model.addAttribute("cursoSeleccionado", cursoId);

        var examenes = examenService.listarPorCurso(cursoId);

        model.addAttribute("examenes", examenes);

        Double promedio =
                examenService.calcularPromedioPorCurso(cursoId);

        model.addAttribute("promedioCurso", promedio);

        var usuario = usuarioService.obtenerUsuarioActual();

        model.addAttribute("notaMaxima", usuario.getNotaMaxima());
        model.addAttribute(
                "notaMinimaAprobatoria",
                usuario.getNotaMinimaAprobatoria()
        );

        // datos para cálculo de aprobación

        double porcentajeEvaluado =
                examenService.calcularPorcentajeEvaluado(cursoId);

        model.addAttribute("porcentajeEvaluado", porcentajeEvaluado);

        double porcentajePendiente =
                examenService.calcularPorcentajePendiente(cursoId);

        model.addAttribute("porcentajePendiente", porcentajePendiente);

        double notaNecesaria =
                examenService.calcularNotaNecesariaParaAprobar(cursoId);

        model.addAttribute("notaNecesaria", notaNecesaria);

        return "examenes";
    }


    // =========================
    // GUARDAR EXAMEN
    // =========================

    @PostMapping("/guardar")
    public String guardarExamen(
            @RequestParam String nombre,
            @RequestParam String fecha,
            @RequestParam(required = false) Double nota,
            @RequestParam Double porcentaje,
            @RequestParam Long cursoId,
            @RequestParam(defaultValue = "PENDIENTE") String estado,
            RedirectAttributes redirectAttributes) {

        try {

            examenService.guardar(
                    nombre,
                    LocalDate.parse(fecha),
                    nota,
                    porcentaje,
                    cursoId,
                    estado
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/examenes?cursoId=" + cursoId;
    }


    // =========================
    // EDITAR NOTA (SLIDER)
    // =========================

    @PostMapping("/editar")
    @ResponseBody
    public Map<String, Object> editarNota(
            @RequestParam Long id,
            @RequestParam Double nota,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = examenService.buscarExamenDelUsuarioActual(id)
                .getUsuario();

        Long cursoId = examenService
                .buscarExamenDelUsuarioActual(id)
                .getCurso()
                .getId();

        try {
            examenService.actualizarNota(id, nota);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("promedio", examenService.calcularPromedioPorCurso(cursoId));
        data.put("porcentajeEvaluado", examenService.calcularPorcentajeEvaluado(cursoId));
        data.put("porcentajeRestante", 100 - (Double) data.get("porcentajeEvaluado"));
        data.put("notaNecesaria", examenService.calcularNotaNecesariaParaAprobar(cursoId));
        data.put("notaMinima", usuario.getNotaMinimaAprobatoria());

        return data;

    }


    // =========================
    // ELIMINAR EXAMEN
    // =========================

    @PostMapping("/eliminar/{id}")
    public String eliminarExamen(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Long cursoId = examenService
                .buscarExamenDelUsuarioActual(id)
                .getCurso()
                .getId();

        try {

            examenService.eliminar(id);

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/examenes?cursoId=" + cursoId;
    }

    @PostMapping("/editar-completo")
    public String editarExamenCompleto(
            @RequestParam Long id,
            @RequestParam String nombre,
            @RequestParam String fecha,
            @RequestParam Double porcentaje,
            @RequestParam(required = false) Double nota,
            @RequestParam(defaultValue = "PENDIENTE") String estado,
            RedirectAttributes redirectAttributes) {

        Long cursoId = examenService
                .buscarExamenDelUsuarioActual(id)
                .getCurso()
                .getId();

        try {

            examenService.editarDatosExamen(
                    id,
                    nombre,
                    LocalDate.parse(fecha),
                    porcentaje,
                    nota,
                    estado
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/examenes?cursoId=" + cursoId;
    }

    @PostMapping("/usuario/actualizar-sistema")
    public String actualizarSistema(
            @RequestParam Double notaMaxima,
            @RequestParam Double notaMinimaAprobatoria) {

        Usuario usuario = usuarioService.obtenerUsuarioActual();

        usuario.setNotaMaxima(notaMaxima);
        usuario.setNotaMinimaAprobatoria(notaMinimaAprobatoria);

        usuarioService.guardar(usuario);

        return "redirect:/examenes";
    }
}

