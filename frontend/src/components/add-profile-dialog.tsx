"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Loader2Icon } from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Field,
  FieldError,
  FieldLabel,
} from "@/components/ui/field";
import { useCreateOlxAccount } from "@/hooks/use-olx-accounts";
import { toastMessages } from "@/lib/toast-messages";

const schema = z.object({
  username: z.string().min(2, "Korisničko ime mora imati najmanje 2 znaka."),
  password: z.string().min(4, "Lozinka mora imati najmanje 4 znaka."),
});

type FormValues = z.infer<typeof schema>;

export function AddProfileDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const create = useCreateOlxAccount();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", password: "" },
  });

  const onSubmit = handleSubmit((data) => {
    create.mutate(data, {
      onSuccess: () => {
        toast.success(toastMessages.created);
        reset();
        onOpenChange(false);
      },
      onError: (err) => {
        toast.error(err.message || toastMessages.error);
      },
    });
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Dodaj OLX profil</DialogTitle>
          <DialogDescription>
            Unesite podatke za prijavu na OLX.ba račun.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <Field data-invalid={!!errors.username || undefined}>
            <FieldLabel htmlFor="add-username">Korisničko ime</FieldLabel>
            <Input
              id="add-username"
              placeholder="npr. shop_sarajevo"
              autoComplete="off"
              {...register("username")}
            />
            <FieldError
              errors={errors.username ? [errors.username] : undefined}
            />
          </Field>
          <Field data-invalid={!!errors.password || undefined}>
            <FieldLabel htmlFor="add-password">Lozinka</FieldLabel>
            <Input
              id="add-password"
              type="password"
              placeholder="••••••••"
              autoComplete="new-password"
              {...register("password")}
            />
            <FieldError
              errors={errors.password ? [errors.password] : undefined}
            />
          </Field>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={create.isPending}
            >
              Otkaži
            </Button>
            <Button type="submit" disabled={create.isPending}>
              {create.isPending && (
                <Loader2Icon className="mr-2 size-4 animate-spin" />
              )}
              Dodaj
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
