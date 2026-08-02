package com.kinplatform.kin.pipeline;

/**
 * Fábricas de {@link PipelineStage} para los tests de resiliencia (E3).
 */
final class TestPipelineStages {

    private TestPipelineStages() {
    }

    static PipelineStage stage(String name) {
        return stage(name, () -> { });
    }

    static PipelineStage stage(String name, Runnable behavior) {
        return new PipelineStage() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean supports(PipelineContext context) {
                return true;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                behavior.run();
                return context;
            }
        };
    }

    static PipelineStage unsupported(String name) {
        return new PipelineStage() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean supports(PipelineContext context) {
                return false;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                throw new IllegalStateException("no debería ejecutarse");
            }
        };
    }

    static PipelineStage failing(String name, RuntimeException error) {
        return stage(name, () -> { throw error; });
    }

    static PipelineStage slow(String name, long millis) {
        return stage(name, () -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    static PipelineStage completingStage(String name) {
        return new PipelineStage() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean supports(PipelineContext context) {
                return true;
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                context.markCompleted();
                return context;
            }
        };
    }
}
